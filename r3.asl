// mars crazy robot r3 (moves and produces grabage randomly)

/* Initial goal */
	
!createGarbage(slots).

/* Plans */

+!createGarbage(slots) : not garbage(r3) & not coal(r3)
	<- putRandGC(slot);
		nextRandom(slot);
	   !createGarbage(slots).
+!createGarbage(slots).




