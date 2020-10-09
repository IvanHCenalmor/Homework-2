// mars robot 2

//+garbage(r2) : true <- burn(garb).

/* Initial goal */

+garbage(r2) : true
	<- !ensure_burn(garb).

/* Plans */

+!ensure_burn(S) : garbage(r2)
   <- burn(garb);
      !ensure_burn(S).
+!ensure_burn(_).
