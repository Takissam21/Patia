(define (domain pursuit-evasion)
  (:requirements :strips :typing)
  (:types
    pursuer
    node
  )

  (:predicates
    (edge ?from - node ?to - node)
    (at ?p - pursuer ?n - node)
    (cleared ?n - node)
  )

  (:action move
    :parameters (?p - pursuer ?from - node ?to - node)
    :precondition (and
      (at ?p ?from)
      (edge ?from ?to)
    )
    :effect (and
      (not (at ?p ?from))
      (at ?p ?to)
      (cleared ?to)
    )
  )
)
