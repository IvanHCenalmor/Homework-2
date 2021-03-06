// mars robot 4 (carries the coal to r2)

/* Initial beliefs */

at(P) :- pos(P,X,Y) & pos(r4,X,Y).

/* Initial goal */

!check(slots).

/* Plans */

+!check(slots) : not coal(r4)
   <- next4(slot);
      !check(slots).
+!check(slots).


@lg[atomic]
+coal(r4) : not .desire(carry_to(r2))
   <- !carry_to(r2).

+!carry_to(R)
   <- // remember where to go back
      ?pos(r4,X,Y);
      -+pos(last,X,Y);

      // carry coal to r2
      !take(coal,R);

      // goes back and continue to check
      !at(last);
      !check(slots).

+!take(S,L) : true
   <- pick(S);
      !at(L);
      drop(S).

+!at(L) : at(L).
+!at(L) <- ?pos(L,X,Y);
           move_towards_r4(X,Y);
           !at(L).

