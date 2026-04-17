(define (problem sokoban-test-01)
  (:domain sokoban)

  (:objects
    c1 c2 c3 - cell
    b1 - box
  )

  (:init
    (player-at c1)
    (box-at b1 c2)
    (clear c3)

    (right c1 c2)
    (right c2 c3)
  )

  (:goal
    (and
      (box-at b1 c3)
    )
  )
)
