package net.bytten.metazelda.viewer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;

import net.bytten.metazelda.Bounds;
import net.bytten.metazelda.Edge;
import net.bytten.metazelda.IDungeon;
import net.bytten.metazelda.Room;
import net.bytten.metazelda.Symbol;
import net.bytten.metazelda.util.Coords;

public class GridDungeonView implements IDungeonView {

    public void drawRoom(Graphics2D g, double scale, Room room) {
        
        int cx = (int)(room.getCenter().x * scale + scale/2),
            cy = (int)(room.getCenter().y * scale + scale/2);
        
        g.setColor(Color.getHSBColor(0.6f - (float)room.getIntensity()*0.6f,
                0.7f, 1.0f));
        
        g.fillOval((int)(cx - scale*0.25),
                (int)(cy - scale*0.25),
                (int)(scale/2), (int)(scale/2));
        
        g.setColor(Color.BLACK);
        
        g.drawOval((int)(cx - scale*0.25),
                (int)(cy - scale*0.25),
                (int)(scale/2), (int)(scale/2));
        
        if (room.isGoal()) {
            g.drawOval((int)(cx - scale*0.2),
                    (int)(cy - scale*0.2),
                    (int)(scale * 0.4), (int)(scale * 0.4));
        }
        
        if (room.getItem() != null) {
            g.drawString(room.getItem().toString(), cx, cy);
        }
        
        g.drawString(String.format("%.2f", room.getIntensity()), cx-12, cy+16);
    }
    
    public void drawArrow(Graphics2D g, double x1, double y1, double x2,
            double y2) {
        AffineTransform origXfm = g.getTransform();
        
        g.drawLine((int)x1,(int)y1,(int)x2,(int)y2);
        
        double len = 0.1*Math.sqrt((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1)),
            rot = Math.atan2(x2-x1,y1-y2);
        g.translate(x2, y2);
        g.rotate(rot);
        g.drawLine(0,0, (int)(-len*2/3), (int)(len));
        g.drawLine(0,0, (int)(len*2/3), (int)(len));
        
        g.setTransform(origXfm);
    }
    
    protected void drawParentEdge(Graphics2D g, double scale,
            Room parent, Room child) {
        double x1 = parent.getCenter().x*scale + scale/2,
                y1 = parent.getCenter().y*scale + scale/2,
                x2 = child.getCenter().x*scale + scale/2,
                y2 = child.getCenter().y*scale + scale/2;
        double sdy = Math.signum(y2-y1), sdx = Math.signum(x2-x1);
        y1 += sdy * scale/4;
        y2 -= sdy * scale/4;
        x1 += sdx * scale/4;
        x2 -= sdx * scale/4;

        int dx = 0, dy = 0;
        dx += (int)(sdy * scale/10);
        dy += (int)(sdx * scale/10);
        x1 += dx; x2 += dx;
        y1 += dy; y2 += dy;
        
        g.setColor(Color.ORANGE);
        drawArrow(g, x1, y1, x2, y2);
        g.setColor(Color.BLACK);
         
        assert parent.getChildren().contains(child);
    }
    
    public void drawEdges(Graphics2D g, double scale, IDungeon dungeon,
            Room room) {
        g.setColor(Color.BLACK);
        
        for (Edge edge: room.getEdges()) {
            Room nextRoom = dungeon.get(edge.getTargetRoomId());
            Coords coords = room.getCenter(),
                   nextCoords = nextRoom.getCenter();
            
            if (nextRoom.getParent() == room) {
                drawParentEdge(g, scale, room, nextRoom);
            }
                
            double x1 = coords.x*scale + scale/2,
                   y1 = coords.y*scale + scale/2,
                   x2 = nextCoords.x*scale + scale/2,
                   y2 = nextCoords.y*scale + scale/2;
            double sdy = Math.signum(y2-y1), sdx = Math.signum(x2-x1);
            y1 += sdy * scale/4;
            y2 -= sdy * scale/4;
            x1 += sdx * scale/4;
            x2 -= sdx * scale/4;

            if (nextRoom != null && Symbol.equals(edge.getSymbol(),
                    nextRoom.getEdge(room.id).getSymbol())) {
                // Bidirectional edge
                // avoid drawing twice:
                if (coords.compareTo(nextCoords) > 0) continue;
                
                g.drawLine((int)x1, (int)y1, (int)x2, (int)y2);
                
                double midx = (x1+x2)/2,
                       midy = (y1+y2)/2;
                if (edge.getSymbol() != null) {
                    g.drawString(edge.getSymbol().toString(),
                            (int)midx, (int)midy);
                }
            } else {
                // Unidirectional edge
                int dx = 0, dy = 0;
                dx += (int)(sdy * scale/20);
                dy += (int)(sdx * scale/20);
                x1 += dx; x2 += dx;
                y1 += dy; y2 += dy;
                drawArrow(g, x1, y1, x2, y2);
                
                double midx = (x1+x2)/2,
                       midy = (y1+y2)/2;
                if (dx < 0) midx += dx*1.5;
                if (dy > 0) midy += dy*1.5;
                if (edge.getSymbol() != null) {
                    g.drawString(edge.getSymbol().toString(),
                            (int)midx, (int)midy);
                }
            }
        }
        
    }
    
    @Override
    public void draw(Graphics2D g, Dimension dim, IDungeon dungeon) {
        AffineTransform origXfm = g.getTransform();
        
        // Figure out scale & translation to draw the dungeon at
        synchronized (dungeon) {
            Bounds bounds = dungeon.getExtentBounds();
            double scale = Math.min(((double)dim.width) / bounds.width(),
                    ((double)dim.height) / bounds.height());
            // move the graph into view
            g.translate(-scale * bounds.left, -scale * bounds.top);
            
            for (Room room: dungeon.getRooms()) {
                // draw the edges between rooms
                drawEdges(g, scale, dungeon, room);
            }
            
            for (Room room: dungeon.getRooms()) {
                // Draw the room
                drawRoom(g, scale, room);
            }
        }
        
        g.setTransform(origXfm);
    }

}