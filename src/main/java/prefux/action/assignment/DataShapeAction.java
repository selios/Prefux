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
package prefux.action.assignment;

import java.util.Map;

import prefux.Constants;
import prefux.data.tuple.TupleSet;
import prefux.util.DataLib;
import prefux.visual.VisualItem;

/**
 * <p>
 * Assignment Action that assigns shape values for a group of items based upon
 * a data field. Shape values are simple integer codes that indicate to
 * appropriate renderer instances what shape should be drawn. The default
 * list of shape values is included in the {@link prefux.Constants} class,
 * all beginning with the prefix <code>SHAPE</code>. Of course, clients can
 * always create their own shape codes that are handled by a custom Renderer. 
 * </p>
 * 
 * <p>The data field will be assumed to be nominal, and shapes will
 * be assigned to unique values in the order they are encountered. Note that
 * if the number of unique values is greater than
 * {@link prefux.Constants#SHAPE_COUNT} (when no palette is given) or
 * the length of a specified palette, then duplicate shapes will start
 * being assigned.</p>
 * 
 * <p>This Action only sets the shape field of the VisualItem. For this value
 * to have an effect, a renderer instance that takes this shape value
 * into account must be used (e.g., {@link prefux.render.ShapeRenderer}).
 * </p>
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class DataShapeAction extends ShapeAction {
    
    protected static final int NO_SHAPE = Integer.MIN_VALUE;
    
    protected String m_dataField;
    protected int[]  m_palette;
    
    protected Map    m_ordinalMap;
    
    
    /**
     * Create a new DataShapeAction.
     * @param group the data group to process
     * @param field the data field to base shape assignments on
     */
    public DataShapeAction(String group, String field) {
        super(group, NO_SHAPE);
        m_dataField = field;
    }
    
    /**
     * Create a new DataShapeAction.
     * @param group the data group to process
     * @param field the data field to base shape assignments on
     * @param palette a palette of shape values to use for the encoding.
     * By default, shape values are assumed to be one of the integer SHAPE
     * codes included in the {@link prefux.Constants} class.
     */
    public DataShapeAction(String group, String field, int[] palette) {
        super(group, NO_SHAPE);
        m_dataField = field;
        m_palette = palette;
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * Returns the data field used to encode shape values.
     * @return the data field that is mapped to shape values
     */
    public String getDataField() {
        return m_dataField;
    }
    
    /**
     * Set the data field used to encode shape values.
     * @param field the data field to map to shape values
     */
    public void setDataField(String field) {
        m_dataField = field;
    }
    
    /**
     * This operation is not supported by the DataShapeAction type.
     * Calling this method will result in a thrown exception.
     * @see prefux.action.assignment.ShapeAction#setDefaultShape(int)
     * @throws UnsupportedOperationException
     */
    public void setDefaultShape(int defaultShape) {
        throw new UnsupportedOperationException();
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * @see prefux.action.EncoderAction#setup()
     */
    protected void setup() {
        TupleSet ts = m_vis.getGroup(m_group);
        m_ordinalMap = DataLib.ordinalMap(ts, m_dataField);
    }
    
    /**
     * @see prefux.action.assignment.ShapeAction#getShape(prefux.visual.VisualItem)
     */
    public int getShape(VisualItem item) {
        // check for any cascaded rules first
        int shape = super.getShape(item);
        if ( shape != NO_SHAPE ) {
            return shape;
        }
        
        // otherwise perform data-driven assignment
        Object v = item.get(m_dataField);
        int idx = ((Integer)m_ordinalMap.get(v)).intValue();

        if ( m_palette == null ) {
            return idx % Constants.SHAPE_COUNT;
        } else {
            return m_palette[idx % m_palette.length];
        }
    }
    
} // end of class DataShapeAction
