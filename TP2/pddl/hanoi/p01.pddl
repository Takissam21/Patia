(define (problem hanoi-3-disks)
  (:domain hanoi)

  (:objects
    d1 d2 d3 - disk
    p1 p2 p3 - peg
  )

  (:init
    (smaller d1 d2)
    (smaller d1 d3)
    (smaller d2 d3)

    (on-disk d1 d2)
    (on-disk d2 d3)
    (on d3 p1)

    (clear-disk d1)
    (clear-peg p2)
    (clear-peg p3)
  )

  (:goal
    (and
      (on-disk d1 d2)
      (on-disk d2 d3)
      (on d3 p3)
    )
  )
)