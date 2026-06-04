# 🧠 Feature MVP — Addiction + AddictionLog (TheLifeDiary)

## Project Context

TheLifeDiary is a SaaS focused on well-being, self-knowledge, overcoming addictions, and personal improvement.

The architecture follows Domain-Driven Design (DDD), using Java, Spring Boot, PostgreSQL, and rich domain entities.

There is already a habits module composed of:

* Habit
* HabitLog

Now we need to design the addiction module, which has similarities to habits, but represents a behavior that the user
wants to avoid or overcome.

---

# 🎯 Feature Objective

To allow the user to:

* Register an addiction they want to combat.

* Record relapses.

* Record successful days without relapses.

* View their current streak of addiction-free days.

* Track their progress over time.

The goal is not to punish the user, but to provide support and awareness.

---

# 🧱 Domain Entities

## Addiction

Represents an addiction or behavior that the user wishes to overcome.

### Responsibilities

* Belong to a user.

* Define the behavior to be combated.

* Store basic information about the addiction.

### Fields

* id
* user_id
* title
* description (optional)
* category
* start_date

### Examples

* Smoking
* Alcohol
* Social media
* Gambling
* Pornography
* Compulsive shopping

### Rules

* Must belong to a user.

* Must have a title.

* Must have a category.

* Must have a start date.

---

## AddictionLog

Represents a daily event related to the addiction.

Unlike HabitLog, here the focus is on recording relapses and monitoring recovery.

### Fields

* id
* addiction_id
* date
* relapsed
* note (optional)

### Meaning

#### relapsed = true

The user relapsed that day.

#### relapsed = false

The user recorded that they remained relapse-free that day.

### Rules

* Only one log per day for each addiction.

* The following constraint must exist:

UNIQUE(addiction_id, date)

---

# 📊 Addiction Category

Create enum:

```java public enum AddictionCategory {
/* SUBSTANCE,
DIGITAL,
GAMBLING,
SEXUAL,
SHOPPING,
FOOD,
OTHER
}
*/
```

### Examples

#### SUBSTANCE

* cigarettes
* alcohol

#### DIGITAL

* social media
* short videos

#### GAMBLING

* gambling
* casino

#### SEXUAL

* pornography

#### SHOPPING

* compulsive shopping

#### FOOD

* binge eating

#### OTHER

* other cases

````

---

# 🧠 Concept Main metric

Habit:

> I want to accomplish something.

Addiction:

> I want to avoid something.

---

# 🔥 Metric Difference

Habit:

- streak increases when completed = true

Addiction:

- streak increases when relapsed = false

---

# 🏆 Sobriety Concept

The main metric of the module will be:

```text
Current Sobriety Streak

````

Representing:

> How many consecutive days the user has been without relapses.

---

# ⚙️ Use Cases

## CreateAddiction

Responsible for:

* validating data
* creating Addiction
* persisting entity

---

## RegisterAddictionLog

Responsible for:

* registering relapse
* registering daily success
* creating or updating AddictionLog

---

## GetAddictionLogs

Responsible for:

* returning history
* updating timeline

---

## GetCurrentSobrietyStreak

Responsible for:

* calculating current streak without relapses

---

# 🧠 Business Rules

## Addiction

* user required
* title required
* category required
* startDate required

---

## AddictionLog

* addiction required
* date required
* relapsed Required

---

## Sobriety Streak

A streak is interrupted when:

* relapsed = true
* there is a gap in the sequence of days

---

# 📊 Database Structure

## addictions

* id
* user_id
* title
* description
* addiction_category
* start_date

---

## addiction_logs

* id
* addiction_id
* date
* relapsed
* note

Constraint:

```sql.UNIQUE(addiction_id, date)

```

---

# 🚀 Implementation Order

## Phase 1

Domain

* Addiction
* AddictionLog

* AddictionCategory

---

## Phase 2

Use Cases

* CreateAddiction
* RegisterAddictionLog
* GetCurrentSobrietyStreak

---

## Phase 3

Persistence

* JPA Entities
* Repositories
* Constraints

---

## Phase 4

API

* REST endpoints
* DTOs
* Pagination

---

# 🎯 Success Criteria

The MVP is ready when:

* user can register an addiction
* user can register relapses
* user can register days without relapses
* system correctly calculates current sobriety
* history can be consulted

---

# 💡 Feature Philosophy

Habit:

> Build positive behaviors.

Addiction:

> Reduce destructive behaviors.

Both share the same philosophy as TheLifeDiary:

> help the user evolve one day at a time.