import jason.asSyntax.*;
import jason.environment.Environment;
import jason.environment.grid.GridWorldModel;
import jason.environment.grid.GridWorldView;
import jason.environment.grid.Location;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.util.Random;
import java.util.logging.Logger;

public class MarsEnv extends Environment {

    public static final int GSize = 10; // grid size
    public static final int GARB  = 16; // garbage code in grid model
	public static final int COAL = 8; // coal code in grid model

    public static final Term    ns = Literal.parseLiteral("next(slot)");
    public static final Term    pg = Literal.parseLiteral("pick(garb)");
    public static final Term    dg = Literal.parseLiteral("drop(garb)");
    public static final Term    bg = Literal.parseLiteral("burn(garb)");
	
	//New terms for robot r3
	public static final Term    nrs = Literal.parseLiteral("nextRandom(slot)");
	public static final Term    prgc = Literal.parseLiteral("putRandGC(slot)");

    public static final Literal g1 = Literal.parseLiteral("garbage(r1)");
    public static final Literal g2 = Literal.parseLiteral("garbage(r2)");
    public static final Literal g3 = Literal.parseLiteral("garbage(r3)");
	
	//New terms for robot r4
	public static final Literal c4 = Literal.parseLiteral("coal(r4)");
	public static final Term    pc = Literal.parseLiteral("pick(coal)");
    public static final Term    dc = Literal.parseLiteral("drop(coal)");
	public static final Term	ns4 = Literal.parseLiteral("next4(slot)");

    static Logger logger = Logger.getLogger(MarsEnv.class.getName());

    private MarsModel model;
    private MarsView  view;

    @Override
    public void init(String[] args) {
        model = new MarsModel();
        view  = new MarsView(model);
        model.setView(view);
        updatePercepts();
    }

    @Override
    public boolean executeAction(String ag, Structure action) {
        logger.info(ag+" doing: "+ action);
        try {
            if (action.equals(ns)) {
                model.nextSlot();
            } else if (action.getFunctor().equals("move_towards")) {
                int x = (int)((NumberTerm)action.getTerm(0)).solve();
                int y = (int)((NumberTerm)action.getTerm(1)).solve();
                model.moveTowards(x,y);
            } else if (action.equals(pg)) {
                model.pickGarb();
            } else if (action.equals(dg)) {
                model.dropGarb();
            } else if (action.equals(bg)) {
                model.burnGarb();
            } // Actions for r3
			  else if (action.equals(nrs)) {
                model.nextRandomSlot();
            } else if (action.equals(prgc)) {
                model.putRandGarbCoal();
            } // Actions for r4
			  else if (action.equals(pc)) {
				model.pickCoal();
			} else if (action.equals(dc)) {
				model.dropCoal();
			} else if (action.equals(ns4)) {
				model.nextSlotR4();
			} else if (action.getFunctor().equals("move_towards_r4")) {
                int x = (int)((NumberTerm)action.getTerm(0)).solve();
                int y = (int)((NumberTerm)action.getTerm(1)).solve();
                model.moveTowardsR4(x,y);
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        updatePercepts();

        try {
            Thread.sleep(200);
        } catch (Exception e) {}
        informAgsEnvironmentChanged();
        return true;
    }

    /** creates the agents perception based on the MarsModel */
    void updatePercepts() {
        clearPercepts();

        Location r1Loc = model.getAgPos(0);
        Location r2Loc = model.getAgPos(1);
		Location r3Loc = model.getAgPos(2);
		Location r4Loc = model.getAgPos(3);
		
        Literal pos1 = Literal.parseLiteral("pos(r1," + r1Loc.x + "," + r1Loc.y + ")");
        Literal pos2 = Literal.parseLiteral("pos(r2," + r2Loc.x + "," + r2Loc.y + ")");
		Literal pos3 = Literal.parseLiteral("pos(r3," + r3Loc.x + "," + r3Loc.y + ")");
		Literal pos4 = Literal.parseLiteral("pos(r4," + r4Loc.x + "," + r4Loc.y + ")");

        addPercept(pos1);
        addPercept(pos2);
		addPercept(pos3);
		addPercept(pos4);

        if (model.hasObject(GARB, r1Loc)) {
            addPercept(g1);
        }
        if (model.hasObject(GARB, r2Loc)) {
            addPercept(g2);
        }
		if (model.hasObject(COAL, r4Loc)) {
			addPercept(c4);	
		}
    }

    class MarsModel extends GridWorldModel {

        public static final int MErr = 2; // max error in pick garb
        int nerr; // number of tries of pick garb
		int nberr; //number of tries of burning the garb
        boolean r1HasGarb = false; // whether r1 is carrying garbage or not
		boolean r4HasCoal = false; // whether r4 is carrying garbage or not
		
		int coalCharges = 0;

        Random random = new Random(System.currentTimeMillis());

        private MarsModel() {
			//GrindWorldModel constructor -> (int width, int heigth, int numsOfAgents)
            super(GSize, GSize, 4);

            // initial location of agents
            try {
                setAgPos(0, random.nextInt(GSize),random.nextInt(GSize));

				
                Location r2Loc = new Location(random.nextInt(GSize), random.nextInt(7));
                setAgPos(1, r2Loc);

				// Lets put the thrid agent also in a random position 
				setAgPos(2, random.nextInt(GSize),random.nextInt(GSize));
				
				setAgPos(3, random.nextInt(GSize),random.nextInt(GSize));
				
            } catch (Exception e) {
                e.printStackTrace();
            }

			//initial location of coal
			add(COAL, 0, 0);
			add(COAL, 1, 1);
			
            // initial location of garbage
            add(GARB, 3, 0);
            add(GARB, GSize-1, 0);
            add(GARB, 1, 2);
            add(GARB, 0, GSize-2);
            add(GARB, GSize-1, GSize-1);
			add(GARB,random.nextInt(GSize),random.nextInt(GSize));
			add(GARB,random.nextInt(GSize),random.nextInt(GSize));
			
			
        }

        void nextSlot() throws Exception {
            Location r1 = getAgPos(0);
			r1.y++;
			if (r1.y == getHeight()) {
                r1.y = 0;
                r1.x++;
            }
            // finished searching the whole grid
            if (r1.x == getWidth()) {
                r1.x=0;
				r1.y=0;
            }
            setAgPos(0, r1);
            setAgPos(1, getAgPos(1)); // just to draw it in the view
			
        }
		
		void nextSlotR4() throws Exception{
			Location r4 = getAgPos(3);
			r4.x++;
			if (r4.x == getWidth()) {
                r4.x = 0;
                r4.y++;
            }
            // finished searching the whole grid
            if (r4.y == getHeight()) {
                r4.x=0;
				r4.y=0;
            }
            setAgPos(3, r4);
            setAgPos(3, getAgPos(3)); // just to draw it in the view
		}
		
		void nextRandomSlot() throws Exception {
			Location r3 = getAgPos(2);
			
			int movOption = random.nextInt(8);
			
			switch(movOption){
				case 0:
					r3.x++;
					break;
				case 1:
					r3.x--;
					break;
				case 2:
					r3.y++;
					break;
				case 3:
					r3.y--;
					break;
				case 4:
					r3.x++;
					r3.y++;
					break;
				case 5:
					r3.x++;
					r3.y--;
					break;
				case 6:
					r3.x--;
					r3.y++;
					break;
				case 7:
					r3.x--;
					r3.y--;
					break;
			}
			
			// Now it will look if is out of the scenerio
			if (r3.y == getHeight()) {
                r3.y = 0;
            }
			if (r3.y == -1) {
                r3.y = getHeight() - 1;
            }
			if (r3.x == getWidth()) {
                r3.x = 0;
            }
			if (r3.x == -1) {
				r3.x = getWidth() - 1;
			}
			
			setAgPos(2, r3);
            setAgPos(2, getAgPos(2)); // just to draw it in the view
		}

        void moveTowards(int x, int y) throws Exception {
            Location r1 = getAgPos(0);
            if (r1.x < x)
                r1.x++;
            else if (r1.x > x)
                r1.x--;
            if (r1.y < y)
                r1.y++;
            else if (r1.y > y)
                r1.y--;
            setAgPos(0, r1);
            setAgPos(1, getAgPos(1)); // just to draw it in the view
        }
		
		void moveTowardsR4(int x, int y) throws Exception {
            Location r4 = getAgPos(3);
            if (r4.x < x)
                r4.x++;
            else if (r4.x > x)
                r4.x--;
            if (r4.y < y)
                r4.y++;
            else if (r4.y > y)
                r4.y--;
            setAgPos(3, r4);
            setAgPos(1, getAgPos(1)); // just to draw it in the view
        }

        void pickGarb() {
            // r1 location has garbage
            if (model.hasObject(GARB, getAgPos(0))) {
                // sometimes the "picking" action doesn't work
                // but never more than MErr times
                if (random.nextBoolean() || nerr == MErr) {
                    remove(GARB, getAgPos(0));
                    nerr = 0;
                    r1HasGarb = true;
                } else {
                    nerr++;
                }
            }
        }
        void dropGarb() {
            if (r1HasGarb) {
                r1HasGarb = false;
                add(GARB, getAgPos(0));
            }
        }
		
		void putRandGarbCoal() {
			if (random.nextInt(10) == 0) {
				if(random.nextInt(2) == 0){
					add(GARB, getAgPos(2));
				}else{
					add(COAL, getAgPos(2));
				}
			}
		}
		void pickCoal() {
            if (model.hasObject(COAL, getAgPos(3))) {
                    remove(COAL, getAgPos(3));
                    r4HasCoal = true;
            }
        }
        void dropCoal() {
            if (r4HasCoal) {
				coalCharges += 2;
                r4HasCoal = false;
            }
        }
		
		
        /* Old burnGarb function */
		/*void burnGarb() {
            // r2 location has garbage
            if (model.hasObject(GARB, getAgPos(1))) {
                remove(GARB, getAgPos(1));
            }
        }*/
		
		void burnGarb() {
            // r2 location has to have garbage and coal to burn it 
            if (model.hasObject(GARB, getAgPos(1)) && coalCharges > 0) {
				if (random.nextBoolean() || nberr == MErr) {
					remove(GARB, getAgPos(1));
					coalCharges -= 1;
					nberr = 0;
				} else {
					nberr++;
				}
			}
        }
    }

    class MarsView extends GridWorldView {

        public MarsView(MarsModel model) {
            super(model, "Mars World", 600);
            defaultFont = new Font("Arial", Font.BOLD, 18); // change default font
            setVisible(true);
            repaint();
        }

        /** draw application objects */
        @Override
        public void draw(Graphics g, int x, int y, int object) {
            switch (object) {
				case MarsEnv.COAL:
					drawCoal(g, x, y);
					break;
				case MarsEnv.GARB:
					drawGarb(g, x, y);
					break;
		
            }
        }

        @Override
        public void drawAgent(Graphics g, int x, int y, Color c, int id) {
            String label = "R"+(id+1);
            c = Color.blue;
            if (id == 0) {
                c = Color.yellow;
                if (((MarsModel)model).r1HasGarb) {
                    label += " - G";
                    c = Color.orange;
                }
            }
			if (id == 2) {
				c = Color.green;	
			}
			
			if (id == 3) {
				c = Color.red;	
				if (((MarsModel)model).r4HasCoal) {
					label += " - C";
                    c = Color.pink;
				}
			}
			
            super.drawAgent(g, x, y, c, -1);
            if (id == 0) {
                g.setColor(Color.black);
            } else {
                g.setColor(Color.white);
            }
            super.drawString(g, x, y, defaultFont, label);
            //repaint();
        }

        public void drawGarb(Graphics g, int x, int y) {
            super.drawObstacle(g, x, y);
            g.setColor(Color.white);
            drawString(g, x, y, defaultFont, "G");
        }

		public void drawCoal(Graphics g, int x, int y) {
			super.drawObstacle(g, x, y);
            g.setColor(Color.red);
            drawString(g, x, y, defaultFont, "C");
		}
    }
}
