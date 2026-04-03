(define (domain taquin)
  (:requirements :strips :typing)

  (:types
    tile cell
  )

  (:predicates
    (at ?t - tile ?c - cell)
    (empty ?c - cell)
    (adjacent ?c1 - cell ?c2 - cell)
  )

  (:action move
    :parameters (?t - tile ?from - cell ?to - cell)
    :precondition (and
      (at ?t ?from)
      (empty ?to)
      (adjacent ?from ?to)
    )
    :effect (and
      (not (at ?t ?from))
      (at ?t ?to)
      (empty ?from)
      (not (empty ?to))
    )
  )
)
