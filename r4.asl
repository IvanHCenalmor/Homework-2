/* Initial beliefs */

/* Initial goal */
	
!collectCoal(slots).

/* Plans */
+!collectCoal(slots) : not coal(r4)
   <- nextC(slot);
      !collectCoal(slots).
+!collectCoal(slots).
