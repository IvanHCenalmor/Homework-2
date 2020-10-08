// mars crazy robot r3 (moves and produces grabage randomly)

/* Initial beliefs */

/* Initial goal */
	
!createGarbage(slots).

/* Plans */

+!createGarbage(slots) : true
	<- nextRandom(slot);
	   !createGarbage(slots).
+!createGarbage(slots).


