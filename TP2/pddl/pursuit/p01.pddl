(define (problem pe-p01)
  (:domain pursuit-evasion)

  (:objects
    p1 p2 - pursuer
    n1 n2 n3 n4 n5 - node
  )

  (:init
    ;; Graphe non orienté
    (edge n1 n2)
    (edge n2 n1)

    (edge n2 n3)
    (edge n3 n2)

    (edge n2 n4)
    (edge n4 n2)

    (edge n2 n5)
    (edge n5 n2)

    (edge n4 n5)
    (edge n5 n4)

    ;; Positions initiales des poursuivants
    (at p1 n1)
    (at p2 n3)

    ;; Nœuds déjà explorés au départ
    (cleared n1)
    (cleared n3)
  )

  (:goal
    (and
      (cleared n1)
      (cleared n2)
      (cleared n3)
      (cleared n4)
      (cleared n5)
    )
  )
)
