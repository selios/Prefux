/*  
 * Copyright (c) 2004-2013 Regents of the University of California.
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3.  Neither the name of the University nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 * OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * 
 * Copyright (c) 2014 Martin Stockhammer
 */
/**
 * Copyright (c) 2004-2006 Regents of the University of California.
 * See "LICENSE.txt" for licensing terms.
 */
package prefux.util.display;

import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;

import prefux.Display;
import prefux.SwingDisplay;
import prefux.util.io.IOLib;

/**
 * Paints a background image in a display. The image can either pan and zoom
 * along with the display or stay stationary. Additionally, the image can
 * be optionally tiled across the Display space. This class is used by
 * the {@link prefux.Display} class in response to the
 * {@link prefux.Display#setBackgroundImage(Image, boolean, boolean)} and
 * {@link prefux.Display#setBackgroundImage(String, boolean, boolean)}
 * methods.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class BackgroundPainter implements PaintListener {

    private static final double THRESH = 0.01;
    
    private Image m_img;
    private boolean m_fixed;
    private boolean m_tiled;
    
    private AffineTransform m_identity;
    private Clip m_clip;

    /**
     * Create a new BackgroundPainter.
     * @param imageLocation a location String of where to retrieve the
     * image file from. Uses
     * {@link prefux.util.io.IOLib#urlFromString(String)} to resolve
     * the String.
     * @param fixed true if the background image should stay in a fixed
     * position, invariant to panning, zooming, or rotation; false if
     * the image should be subject to view transforms
     * @param tile true to tile the image across the visible background,
     * false to only include the image once
     */
    public BackgroundPainter(String imageLocation, boolean fixed, boolean tile)
    {
        this(Toolkit.getDefaultToolkit()
                .getImage(IOLib.urlFromString(imageLocation)),
             fixed, tile);        
    }
    
    /**
     * Create a new BackgroundPainter.
     * @param image the background Image
     * @param fixed true if the background image should stay in a fixed
     * position, invariant to panning, zooming, or rotation; false if
     * the image should be subject to view transforms
     * @param tile true to tile the image across the visible background,
     * false to only include the image once
     */
    public BackgroundPainter(Image image, boolean fixed, boolean tile) {
        m_img = image;
        
        // make sure the image is completely loaded
        MediaTracker mt = new MediaTracker(new Container());
        mt.addImage(m_img, 0);
        try {
            mt.waitForID(0);
        } catch ( Exception e ) { e.printStackTrace(); }
        mt.removeImage(m_img, 0);
        
        m_fixed = fixed;
        m_tiled = tile;
    }
    
    /**
     * Paint the background.
     * @see prefux.util.display.PaintListener#prePaint(prefux.Display, java.awt.Graphics2D)
     */
    public void prePaint(Display di, Graphics2D g) {
    	SwingDisplay d = (SwingDisplay) di;
        AffineTransform at = g.getTransform();
        boolean translate = isTranslation(at);
        
        if ( m_fixed || translate )
        {
            // if the background is fixed, we can unset the transform.
            // if we have no scaling component, we draw the image directly
            //  rather than run it through the transform.
            //  this avoids rendering artifacts on Java 1.5 on Win32.
            
            int tx = m_fixed ? 0 : (int)at.getTranslateX();
            int ty = m_fixed ? 0 : (int)at.getTranslateY();
            
            g.setTransform(getIdentity());
            if ( m_tiled ) {
                // if tiled, compute visible background region and draw tiles
                int w = new Double(d.getWidth()).intValue(),  iw = m_img.getWidth(null);
                int h = new Double(d.getHeight()).intValue(), ih = m_img.getHeight(null);
                
                int sx = m_fixed ? 0 : tx%iw;
                int sy = m_fixed ? 0 : ty%ih;
                if ( sx > 0 ) sx -= iw;
                if ( sy > 0 ) sy -= ih;
                
                for ( int x=sx; x<w-sx; x+=iw ) {
                    for ( int y=sy; y<h-sy; y+=ih )
                        g.drawImage(m_img, x, y, null);
                }
            } else {
                // if not tiled, simply draw the image at the translated origin
                g.drawImage(m_img, tx, ty, null);
            }
            g.setTransform(at);
        }
        else
        {
            // run the image through the display transform
            if ( m_tiled ) {
                int iw = m_img.getWidth(null);
                int ih = m_img.getHeight(null);
                
                // get the screen region and map it into item-space
                Clip c = getClip();
                c.setClip(0, 0, d.getWidth(), d.getHeight());
                c.transform(d.getInverseTransform());
                
                // get the bounding region for image tiles
                int w = (int)Math.ceil(c.getWidth());
                int h = (int)Math.ceil(c.getHeight());
                int tx = (int)c.getMinX();
                int ty = (int)c.getMinY();
                int dw = tx%iw + iw;
                int dh = ty%ih + ih;
                tx -= dw; w += dw;
                ty -= dh; h += dh;
                
                // draw the image tiles
                for ( int x=tx; x<tx+w; x+=iw ) {
                    for ( int y=ty; y<ty+h; y+=ih )
                        g.drawImage(m_img, x, y, null);
                }
            } else {
                // if not tiled, simply draw the image
                g.drawImage(m_img, 0, 0, null);
            }
        }
        
    }

    /**
     * Check if the given AffineTransform is a translation
     * (within thresholds -- see {@link #THRESH}.
     */
    private static boolean isTranslation(AffineTransform at) {
        return ( Math.abs(at.getScaleX()-1.0) < THRESH &&
                 Math.abs(at.getScaleY()-1.0) < THRESH &&
                 Math.abs(at.getShearX())     < THRESH &&
                 Math.abs(at.getShearY())     < THRESH );
    }

    /**
     * Get an identity transform (creating it if necessary)
     */
    private AffineTransform getIdentity() {
        if ( m_identity == null )
            m_identity = new AffineTransform();
        return m_identity;
    }

    /**
     * Get a clip instance (creating it if necessary)
     */
    private Clip getClip() {
        if ( m_clip == null )
            m_clip = new Clip();
        return m_clip;
    }
    
    /**
     * Does nothing.
     * @see prefux.util.display.PaintListener#postPaint(prefux.Display, java.awt.Graphics2D)
     */
    public void postPaint(Display d, Graphics2D g) {
        // do nothing
    }

} // end of class BackgroundPainter
