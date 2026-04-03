(define (domain hanoi)
  (:requirements :strips :typing)

  (:types
    disk peg
  )

  (:predicates
    (on ?d - disk ?p - peg)
    (on-disk ?d1 - disk ?d2 - disk)
    (clear-disk ?d - disk)
    (clear-peg ?p - peg)
    (smaller ?d1 - disk ?d2 - disk)
  )

  (:action move-peg-to-peg
    :parameters (?d - disk ?from - peg ?to - peg)
    :precondition (and
      (on ?d ?from)
      (clear-disk ?d)
      (clear-peg ?to)
    )
    :effect (and
      (not (on ?d ?from))
      (on ?d ?to)
      (clear-peg ?from)
      (not (clear-peg ?to))
    )
  )

  (:action move-peg-to-disk
    :parameters (?d1 - disk ?from - peg ?d2 - disk)
    :precondition (and
      (on ?d1 ?from)
      (clear-disk ?d1)
      (clear-disk ?d2)
      (smaller ?d1 ?d2)
    )
    :effect (and
      (not (on ?d1 ?from))
      (on-disk ?d1 ?d2)
      (clear-peg ?from)
      (not (clear-disk ?d2))
    )
  )

  (:action move-disk-to-peg
    :parameters (?d1 - disk ?from - disk ?to - peg)
    :precondition (and
      (on-disk ?d1 ?from)
      (clear-disk ?d1)
      (clear-peg ?to)
    )
    :effect (and
      (not (on-disk ?d1 ?from))
      (on ?d1 ?to)
      (clear-disk ?from)
      (not (clear-peg ?to))
    )
  )

  (:action move-disk-to-disk
    :parameters (?d1 - disk ?from - disk ?to - disk)
    :precondition (and
      (on-disk ?d1 ?from)
      (clear-disk ?d1)
      (clear-disk ?to)
      (smaller ?d1 ?to)
    )
    :effect (and
      (not (on-disk ?d1 ?from))
      (on-disk ?d1 ?to)
      (clear-disk ?from)
      (not (clear-disk ?to))
    )
  )
)