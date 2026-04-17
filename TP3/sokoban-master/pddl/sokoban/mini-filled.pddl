(define (problem sokoban-mini-filled)
  (:domain sokoban)

  (:objects
    c1 c2 c3 - cell
    b1 - box
  )

  (:init
    (player-at c1)
    (box-at b1 c2)
    (goal c3)
    (clear c3)

    (right c1 c2)
    (right c2 c3)
    (left c2 c1)
    (left c3 c2)
  )

  (:goal
    (and
      (filled c3)
    )
  )
)
