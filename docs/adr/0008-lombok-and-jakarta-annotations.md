# ADR-0008: Lombok and jakarta annotations, scoped by layer

**Status:** Accepted
**Date:** 2026-08-14
**Deciders:** Project maintainers

## Context

Lombok is already a dependency in both `pom.xml` files, with the annotation
processor wired into `maven-compiler-plugin`. `jakarta.validation` is required
by specification §6.3 (input validation through `jakarta.validation` on DTOs)
and `jakarta.persistence` is unavoidable for JPA. So neither is optional; the
only open question is *where* each is allowed.

Two constraints pull against each other:

**§4 rule 1** — the `domain` package contains no `org.springframework.*` or
`jakarta.persistence.*` imports and no framework annotations.

**§13 acceptance criterion** — the ArchUnit test verifying the absence of
framework dependencies in `domain` passes.

Against that, boilerplate is real. Nine entity classes with hand-written
accessors is several hundred lines that say nothing, and a reviewer skims past
them, which is exactly where a typo hides.

There is also a mechanism problem that only becomes visible when attempting to
enforce any of this: **ArchUnit cannot see Lombok.** Lombok's annotations are
`RetentionPolicy.SOURCE` — erased by the compiler, never written to a class
file. ArchUnit analyses bytecode. A rule of the form `domain should not depend
on lombok..` therefore compiles, runs, passes, and guards nothing at all. It is
worse than having no rule, because it reads like protection. That rule was
present in the first draft of `HexagonalArchitectureTest` and has been removed.

## Decision

Both are adopted, scoped by layer, with the scope enforced by whichever
mechanism can actually see the thing being restricted.

| Layer | Lombok | jakarta |
|---|---|---|
| `domain` | `@Getter`, `@ToString` only | **none** |
| `application` | `@RequiredArgsConstructor`, `@Slf4j` | none |
| `infrastructure.adapter.out.persistence` | anything | `jakarta.persistence` — required |
| `infrastructure.adapter.in.web` | anything | `jakarta.validation` — required by §6.3 |
| `infrastructure.config` | anything | as needed |

### Lombok in the domain: two annotations, six bans

Permitted: `@Getter`, `@ToString(of = {...})`. Both are pure boilerplate with no
semantics of their own.

Rejected, each for a specific reason rather than on principle:

| Annotation | Why it is rejected |
|---|---|
| `@Data` | Bundles `@Setter` + `@EqualsAndHashCode` — reopens every field *and* imposes value equality. Both defects at once. |
| `@EqualsAndHashCode` | Value equality. Entities are equal by id (ADR-0001); generated equality makes two snapshots of one order compare as different orders. |
| `@Setter` | State changes go through named methods that validate. `changeQuantity(0)` throws; `setQuantity(0)` succeeds and fails later at flush time with no useful stack trace. |
| `@Value` | Right shape for a value object, but value objects here are records — same guarantees, in the language rather than in a processor. |
| `@AllArgsConstructor` | Parameter order follows field declaration order. Reorder two fields of the same type and every call site silently reorders too, still compiling. A written-out constructor makes that a compile error. |
| `@Builder` | Bypasses the static factories (`Product.create`, `Reservation.open`, `Order.place`) that establish invariants, so it can construct an object the domain considers impossible. Fine in test fixtures and infrastructure DTOs. |

Enforced by `src/main/java/com/volt/<service>/domain/lombok.config`, using
`flagUsage = ERROR`. This runs inside the compiler, where the annotations still
exist, so a violation fails the build at the offending line. It is the only
mechanism that can enforce this — see the ArchUnit note above.

One subtlety worth knowing: `@Getter` on a class generates an accessor for
*every* field, including the `List` backing an aggregate — handing out the live
`ArrayList`. `Cart`, `Order` and `Reservation` therefore carry
`@Getter(AccessLevel.NONE)` on that field plus a hand-written accessor returning
an unmodifiable view. Forgetting this is how encapsulation dies quietly.

The project-wide `lombok.config` also sets `lombok.addLombokGeneratedAnnotation
= true`. JaCoCo 0.8+ skips members annotated `@Generated`, so generated
accessors stop diluting §9's 70% target — which then measures code actually
written rather than rewarding tests that call getters.

### jakarta annotations: everywhere they do something, nowhere they do not

`jakarta.persistence` is confined to `adapter.out.persistence` and
`jakarta.validation` belongs on the request DTOs in `adapter.in.web`, where §6.3
puts it. Both are enforced by ArchUnit, which *can* see them — unlike Lombok,
these have runtime retention and appear in bytecode.

**Neither goes in `domain`**, and the reason is not only the rule:

*Mechanically, it violates a required acceptance criterion.* §13 includes the
ArchUnit domain-purity test. Annotating a domain model with
`jakarta.validation` makes that test fail. This is not a stylistic preference;
it is part of the specification's definition of project success.

*Practically it buys nothing.* A `@NotNull` on a domain field does **exactly
nothing** unless something invokes a `Validator`, and nothing invokes one on
domain objects — Spring validates controller arguments, not aggregates
constructed by application code. Meanwhile the guards already in the constructors fire
unconditionally:

```java
public CartLine(Long id, Long productId, int quantity) {
    if (quantity < 1) {
        throw new IllegalArgumentException("cart line quantity must be positive");
    }
    ...
}
```

A decorative annotation is worse than no annotation, because it reads as
enforcement.

*It also breaks the property the layout exists for.* §9 requires the domain to
be testable without a Spring context, and §4's whole point is that the inner
layers compile with no framework on the classpath. That property is currently
verifiable in one command — `javac` the `domain` and `application` trees with an
empty classpath. Adding jakarta to `domain` ends that.

## Options considered

### Option A: Scoped by layer, enforced per-mechanism (chosen)

**Pros:** Boilerplate disappears where it is genuinely boilerplate. §13 stays
green. Domain still compiles with an empty classpath. Each restriction is
enforced by a mechanism that can actually observe it.

**Cons:** Two `lombok.config` files per service, and the split needs explaining
to anyone new. `@Getter(AccessLevel.NONE)` on collection fields is easy to
forget and silent when forgotten.

### Option B: Lombok and jakarta everywhere, including `domain`

**Pros:** One rule, no exceptions to remember. Fewest lines of code. `@Data` on
every model would delete roughly 400 lines across the two services.

**Cons:** Rejected. Fails §13's ArchUnit criterion outright. `@Data` restores
value equality and public setters, undoing ADR-0001 and every constructor
invariant. Bean validation in the domain is inert. The domain stops being
compilable without a framework, so §9's "testable without starting a Spring
context" becomes an assertion rather than something verifiable.

### Option C: No Lombok at all

**Pros:** Zero magic; the source directly reflects runtime behaviour. No
annotation processor to misconfigure, no IDE plugin required for a checkout to
compile.

**Cons:** Rejected — Lombok is already a project dependency and its cost is
concentrated exactly where it is safe. It would also mean several hundred lines
of accessor that a reviewer skims, which is where mistakes live.

## Trade-off analysis

The useful generalisation is that **a rule is only as good as the mechanism that
can observe it.** Three restrictions here look similar and need three different
enforcers:

| Restriction | Visible to | Because |
|---|---|---|
| No `jakarta.*` in domain | ArchUnit | runtime retention → in bytecode |
| No `@Data` in domain | `lombok.config` | source retention → erased before bytecode |
| Entities are classes, not records | neither, today | needs a custom `ArchCondition` (ADR-0001) |

Picking the wrong enforcer produces a rule that passes forever while the thing
it names goes unchecked. The removed `lombok..` ArchUnit rule was exactly that,
and it would have sat in the suite looking reassuring.

The residual risk in Option A is `@Getter(AccessLevel.NONE)`. Three classes need
it and nothing checks that they have it; forget it on a fourth and the aggregate
starts leaking its internal list with no error anywhere. Mitigation: a test that
asserts `cart.getLines().add(...)` throws — one line, and it fails loudly.

## Consequences

**Easier**
- Accessors and `toString` stop consuming review attention.
- Generated code is excluded from coverage, so §9's 70% means something.
- `@RequiredArgsConstructor` makes constructor injection the path of least
  resistance, which keeps use cases instantiable in plain unit tests.
- The bans fail the build at the offending line with a readable message.

**Harder**
- Lombok requires an IDE plugin. Without it, IntelliJ reports errors on code
  that compiles fine — note this in the README before someone loses an hour.
- Two config files per service, and the reason for the split is not obvious
  from either one.
- `@Getter(AccessLevel.NONE)` is easy to forget and silent when forgotten.

**To revisit**
- If Lombok's IDE friction outweighs the saving, the domain reverts to
  hand-written accessors — a mechanical change, since only `@Getter` and
  `@ToString` are in use.
- Once the suite runs green, add the ArchUnit `ArchCondition` from ADR-0001
  forbidding a record with an `id` field in `domain.model`.

## Action items

1. [x] `lombok.config` per service: `addLombokGeneratedAnnotation`, `stopBubbling`
2. [x] `domain/lombok.config`: `flagUsage = ERROR` for the six banned annotations
3. [x] `@Getter` / `@ToString` on the nine entities; `AccessLevel.NONE` on collections
4. [x] Remove the no-op `lombok..` rule from `HexagonalArchitectureTest`
5. [ ] Test that `getLines()` rejects mutation — the one gap `lombok.config` cannot close
6. [ ] README: note that Lombok needs an IDE plugin
7. [ ] Use `@RequiredArgsConstructor` + `@Slf4j` on every use case implementation
8. [ ] `jakarta.validation` on web request DTOs (§6.3), never on domain models
