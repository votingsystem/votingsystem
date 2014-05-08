package org.votingsystem.admintool;

import org.apache.log4j.Logger;
import org.votingsystem.model.ContextVS;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
* @author jgzornoza
* Licencia: https://github.com/jgzornoza/SistemaVotacion/wiki/Licencia
*/
public class ClosableTabbedPane extends JTabbedPane {
    
    private static Logger logger = Logger.getLogger(ClosableTabbedPane.class);

    private static int CLOSE_TAB_GAP_Y = 18;
    private static int CLOSE_TAB_GAP_X = 8;


    private TabCloseUI closeUI = new TabCloseUI(this);

    private List<String> fileList = new ArrayList<String>();
    

    @Override
    public void paint(Graphics g){
        super.paint(g);
        closeUI.paint(g);
    }
	
    @Override
    public void addTab(String title, Component component) {
        super.addTab(title + "     ", component);
    }
	
    public void addTab(File file, Component component) {
        String tituloTab = file.getName();
        if (fileList.contains(file.getPath())) {
            setSelectedIndex(fileList.indexOf(file.getPath()));
            return;
        } else fileList.add(file.getPath());
        if (tituloTab.length() > 20) tituloTab = tituloTab.substring(0, 20);
        super.addTab(tituloTab + "     ", component);
    }
    
    public int indexOfFile (File file) throws IOException {
        return fileList.indexOf(file.getPath());
    }
                
	
    public String getTabTitleAt(int index) {
        return super.getTitleAt(index).trim();
    }

    private class TabCloseUI implements MouseListener, MouseMotionListener {
        private ClosableTabbedPane  tabbedPane;
        private int closeX = 0 ,closeY = 0, meX = 0, meY = 0;
        private int selectedTab;
        private final int  width = 8, height = 8;
        private Rectangle rectangle = new Rectangle(0,0,width, height);
        private TabCloseUI(){}
        public TabCloseUI(ClosableTabbedPane pane) {

            tabbedPane = pane;
            tabbedPane.addMouseMotionListener(this);
            tabbedPane.addMouseListener(this);
        }
        public void mouseEntered(MouseEvent me) {}
        public void mouseExited(MouseEvent me) {}
        public void mousePressed(MouseEvent me) {}
        public void mouseClicked(MouseEvent me) {}
        public void mouseDragged(MouseEvent me) {}



        public void mouseReleased(MouseEvent me) {
            if(closeUnderMouse(me.getX(), me.getY())){
                boolean isToCloseTab = tabAboutToClose(selectedTab);
                if (isToCloseTab && selectedTab > -1){			
                    tabbedPane.removeTabAt(selectedTab);
                }
                selectedTab = tabbedPane.getSelectedIndex();
            }
        }

        public void mouseMoved(MouseEvent me) {	
            meX = me.getX();
            meY = me.getY();			
            if(mouseOverTab(meX, meY)){
                controlCursor();
                tabbedPane.repaint();
            }
        }

        private void controlCursor() {
            if(tabbedPane.getTabCount()>0)
            if(closeUnderMouse(meX, meY)){
                tabbedPane.setCursor(new Cursor(Cursor.HAND_CURSOR));	
                if(selectedTab > -1)
                        tabbedPane.setToolTipTextAt(selectedTab, 
                        ContextVS.getInstance().getMessage("closeToolTipText") +
                        tabbedPane.getTitleAt(selectedTab));
            }
            else{
                tabbedPane.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
                if(selectedTab > -1)
                        tabbedPane.setToolTipTextAt(selectedTab,"");
            }	
        }

        private boolean closeUnderMouse(int x, int y) {		
            rectangle.x = closeX;
            rectangle.y = closeY;
            return rectangle.contains(x,y);
        }

        public void paint(Graphics g) {
            int tabCount = tabbedPane.getTabCount();
            for(int j = 0; j < tabCount; j++)
                if(tabbedPane.getComponent(j).isShowing()){			
                    int x = tabbedPane.getBoundsAt(j).x + tabbedPane.getBoundsAt(j).width -width - CLOSE_TAB_GAP_X;
                    int y = tabbedPane.getBoundsAt(j).y + CLOSE_TAB_GAP_Y;
                    drawClose(g,x,y);
                    break;
                }
            if(mouseOverTab(meX, meY)){
                drawClose(g,closeX,closeY);
            }
        }

        private void drawClose(Graphics g, int x, int y) {
            if(tabbedPane != null && tabbedPane.getTabCount() > 0){
                Graphics2D g2 = (Graphics2D)g;				
                drawColored(g2, isUnderMouse(x,y)? Color.RED : Color.WHITE, x, y);
            }
        }

        private void drawColored(Graphics2D g2, Color color, int x, int y) {
            g2.setStroke(new BasicStroke(5,BasicStroke.JOIN_ROUND,BasicStroke.CAP_ROUND));
            g2.setColor(Color.BLACK);
            g2.drawLine(x, y, x + width, y + height);
            g2.drawLine(x + width, y, x, y + height);
            g2.setColor(color);
            g2.setStroke(new BasicStroke(3, BasicStroke.JOIN_ROUND, BasicStroke.CAP_ROUND));
            g2.drawLine(x, y, x + width, y + height);
            g2.drawLine(x + width, y, x, y + height);
        }

        private boolean isUnderMouse(int x, int y) {
            if(Math.abs(x-meX)<width && Math.abs(y-meY)<height )
                return  true;		
            return  false;
        }

        private boolean mouseOverTab(int x, int y) {
            int tabCount = tabbedPane.getTabCount();
            for(int j = 0; j < tabCount; j++)
                if(tabbedPane.getBoundsAt(j).contains(meX, meY)){
                    selectedTab = j;
                    closeX = tabbedPane.getBoundsAt(j).x + tabbedPane.getBoundsAt(j).width - width - CLOSE_TAB_GAP_X;
                    closeY = tabbedPane.getBoundsAt(j).y + CLOSE_TAB_GAP_Y;
                    return true;
                }
            return false;
        }

    }

    public boolean tabAboutToClose(int tabIndex) {
        logger.debug("tabAboutToClose tabIndex: " + tabIndex + " - closed "  + fileList.remove(tabIndex));
        return true;
    }
	
}
