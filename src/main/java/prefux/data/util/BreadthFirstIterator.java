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
package prefux.data.util;

import java.util.Iterator;

import prefux.Constants;
import prefux.data.Edge;
import prefux.data.Node;
import prefux.data.Tuple;
import prefux.util.collections.Queue;

/**
 * Provides a distance-limited breadth first traversal over nodes, edges,
 * or both, using any number of traversal "roots".
 *  
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class BreadthFirstIterator implements Iterator {

    protected Queue m_queue = new Queue();
    protected int   m_depth;
    protected int   m_traversal;
    protected boolean m_includeNodes;
    protected boolean m_includeEdges;
    
    /**
     * Create an uninitialized BreadthFirstIterator. Use the
     * {@link #init(Object, int, int)} method to initialize the iterator.
     */
    public BreadthFirstIterator() {
        // do nothing, requires init call
    }
    
    /**
     * Create a new BreadthFirstIterator starting from the given source node.
     * @param n the source node from which to begin the traversal
     * @param depth the maximum graph distance to traverse
     * @param traversal the traversal type, one of
     * {@link prefux.Constants#NODE_TRAVERSAL},
     * {@link prefux.Constants#EDGE_TRAVERSAL}, or
     * {@link prefux.Constants#NODE_AND_EDGE_TRAVERSAL}
     */
    public BreadthFirstIterator(Node n, int depth, int traversal) {
        init(new Node[] {n}, depth, traversal);
    }
    
    /**
     * Create a new BreadthFirstIterator starting from the given source nodes.
     * @param it an Iterator over the source nodes from which to begin the
     * traversal
     * @param depth the maximum graph distance to traverse
     * @param traversal the traversal type, one of
     * {@link prefux.Constants#NODE_TRAVERSAL},
     * {@link prefux.Constants#EDGE_TRAVERSAL}, or
     * {@link prefux.Constants#NODE_AND_EDGE_TRAVERSAL}
     */
    public BreadthFirstIterator(Iterator it, int depth, int traversal) {
        init(it, depth, traversal);
    }
    
    /**
     * Initialize (or re-initialize) this iterator.
     * @param o Either a source node or iterator over source nodes
     * @param depth the maximum graph distance to traverse
     * @param traversal the traversal type, one of
     * {@link prefux.Constants#NODE_TRAVERSAL},
     * {@link prefux.Constants#EDGE_TRAVERSAL}, or
     * {@link prefux.Constants#NODE_AND_EDGE_TRAVERSAL}
     */
    public void init(Object o, int depth, int traversal) {
        // initialize the member variables
        m_queue.clear();
        m_depth = depth;
        if ( traversal < 0 || traversal >= Constants.TRAVERSAL_COUNT )
            throw new IllegalArgumentException(
                    "Unrecognized traversal type: "+traversal);
        m_traversal = traversal;
        m_includeNodes = (traversal == Constants.NODE_TRAVERSAL || 
                traversal == Constants.NODE_AND_EDGE_TRAVERSAL);
        m_includeEdges = (traversal == Constants.EDGE_TRAVERSAL ||
                traversal == Constants.NODE_AND_EDGE_TRAVERSAL);
        
        // seed the queue
        // TODO: clean this up? (use generalized iterator?)
        if ( m_includeNodes ) {
            if ( o instanceof Node ) {
                m_queue.add(o, 0);
            } else {
                Iterator tuples = (Iterator)o;
                while ( tuples.hasNext() )
                    m_queue.add(tuples.next(), 0);
            }
        } else {
            if ( o instanceof Node ) {
                Node n = (Node)o;
                m_queue.visit(n, 0);
                Iterator edges = getEdges(n);
                while ( edges.hasNext() ) {
                    Edge e = (Edge)edges.next();
                    Node nn = e.getAdjacentNode(n);
                    m_queue.visit(nn, 1);
                    if ( m_queue.getDepth(e) < 0 )
                        m_queue.add(e, 1);
                }
            } else {
                Iterator tuples = (Iterator)o;
                while ( tuples.hasNext() ) {
                    // TODO: graceful error handling when non-node in set?
                    Node n = (Node)tuples.next();
                    m_queue.visit(n, 0);
                    Iterator edges = getEdges(n);
                    while ( edges.hasNext() ) {
                        Edge e = (Edge)edges.next();
                        Node nn = e.getAdjacentNode(n);
                        m_queue.visit(nn, 1);
                        if ( m_queue.getDepth(e) < 0 )
                            m_queue.add(e, 1);
                    }
                }
            }
        }
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * @see java.util.Iterator#remove()
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        return !m_queue.isEmpty();
    }

    /**
     * Determines which edges are traversed for a given node.
     * @param n a node
     * @return an iterator over edges incident on the node
     */
    protected Iterator getEdges(Node n) {
        return n.edges(); // TODO: add support for all edges, in links only, out links only
    }
    
    /**
     * Get the traversal depth at which a particular tuple was encountered.
     * @param t the tuple to lookup
     * @return the traversal depth of the tuple, or -1 if the tuple has not
     * been visited by the traversal.
     */
    public int getDepth(Tuple t) {
        return m_queue.getDepth(t);
    }
    
    /**
     * @see java.util.Iterator#next()
     */
    public Object next() {        
        Tuple t = (Tuple)m_queue.removeFirst();

        switch ( m_traversal ) {
        
        case Constants.NODE_TRAVERSAL:
        case Constants.NODE_AND_EDGE_TRAVERSAL:
            for ( ; true; t = (Tuple)m_queue.removeFirst() ) {
                if ( t instanceof Edge ) {
                    return t;
                } else {
                    Node n = (Node)t;
                    int d = m_queue.getDepth(n);
                    
                    if ( d < m_depth ) {
                        int dd = d+1;
                        Iterator edges = getEdges(n);
                        while ( edges.hasNext() ) {
                            Edge e = (Edge)edges.next();
                            Node v = e.getAdjacentNode(n);
                        
                            if ( m_includeEdges && m_queue.getDepth(e) < 0 )
                                m_queue.add(e, dd);
                            if ( m_queue.getDepth(v) < 0 )
                                m_queue.add(v, dd);
                        }
                    }
                    else if ( m_includeEdges && d == m_depth )
                    {
                        Iterator edges = getEdges(n);
                        while ( edges.hasNext() ) {
                            Edge e = (Edge)edges.next();
                            Node v = e.getAdjacentNode(n);
                            int dv = m_queue.getDepth(v);
                            if ( dv > 0 && m_queue.getDepth(e) < 0 ) {
                                m_queue.add(e, Math.min(d,dv));
                            }
                        }
                    }
                    return n;
                }
            }
                
        case Constants.EDGE_TRAVERSAL:
            Edge e = (Edge)t;
            Node u = e.getSourceNode();
            Node v = e.getTargetNode();
            int du = m_queue.getDepth(u);
            int dv = m_queue.getDepth(v);

            if ( du != dv ) {
                Node n = (dv > du ? v : u);
                int  d = Math.max(du, dv);
            
                if ( d < m_depth ) {
                    int dd = d+1;
                    Iterator edges = getEdges(n);
                    while ( edges.hasNext() ) {
                        Edge ee = (Edge)edges.next();
                        if ( m_queue.getDepth(ee) >= 0 )
                            continue; // already visited
            
                        Node nn = ee.getAdjacentNode(n);
                        m_queue.visit(nn, dd);
                        m_queue.add(ee, dd);
                    }
                }
            }
            return e;
        
        default:
            throw new IllegalStateException();
        }
    }

} // end of class BreadthFirstIterator
