# Semantic Modelling

Imixs-Workflow is a powerful BPMN engine that allows organizations to digitize their business processes and simplify the handling of daily tasks. Every workflow describes what a process is about and what needs to be done at each point in time. This is why the names given to Tasks and Events are very important to make a business process successful.

Imixs-Workflow follows an **event-driven modelling approach**: unlike task-oriented engines, a Task represents a state, while an Event represents the action that changes it. Task and event names should consistently reflect this distinction. See also [How to Model with Imixs-BPMN](./howto.md) for the underlying BPMN concepts.

This guide describes a simple semantic concept for naming Tasks and Events correctly and for recognizing the tricky edge cases where the obvious rules do not quite fit. If you are already familiar with the concept, you can jump straight to the [Checklist](#6-checklist) at the end.

## 1. The Basic Principle

In Imixs-Workflow, a **Task** always describes a state a process instance is currently in - not an activity. An **Event** always describes the action that forwards a process instance from one state into another:

1. **The task name describes a state**. Typically a noun phrase, adjective, or participle expressing "where things stand right now."
2. **The event names describe an action**. Typically a verb in the imperative, expressing "what happens now."

A quick test for any name you choose:

- Does it complete the sentence _"The process is currently ..."_? → good Task name.
- Does it complete the sentence _"Now: ..."_, as an instruction to the user? → good Event name.

This pattern is the same regardless of domain. It does not matter whether you model an invoice approval, a hiring process, or a shipment workflow. The underlying question is always: "Is this a state, or is this an action?"

| Domain           | State (Task)          | Action (Event)      |
| ---------------- | --------------------- | ------------------- |
| Invoice Approval | "In Review"           | "Approve"           |
| Recruiting       | "Interview Scheduled" | "Confirm Interview" |
| Shipment         | "In Transit"          | "Mark Delivered"    |

This State ↔ Action logic applies to any process you model. If you find yourself copying task and event names from an existing model instead of asking _"what is the actual state / action here?"_, you have likely lost the semantic thread.

## 2. Naming Tasks

A Task name should let a user immediately recognize the current state of a process instance, without needing to open the form or read the whole process history. Since Task names appear in task lists together with the workflow group (e.g. "Invoice Receipt >> In Review"), they must be understandable on their own, out of context.

A good Task name typically follows one of these patterns:

### 2.1 "In + Noun"

This pattern expresses an **ongoing state**, something is currently happening, and the process instance is waiting for it to conclude.

Examples: _"In Review", "In Approval", "In Execution", "In Clarification"_

This is usually the best choice when a task represents a phase of work that takes time and involves a person actively doing something with the process instance.

### 2.2 Past Participle / Adjective

This pattern expresses a **state that has been reached**, something has already happened, and the process instance now reflects the result of that action.

Examples: _"Approved", "Archived", "Rejected", "Completed"_

This pattern fits well for final or near-final states, where the emphasis is on the outcome rather than on ongoing activity.

### 2.3 Noun Phrase Describing a Waiting State

This pattern expresses that the process instance is **waiting for something** to happen next, often before any activity has started.

Examples: _"New", "Open", "Pending Approval", "Awaiting Payment"_

This pattern is typically used at the beginning of a process, or whenever a process instance is queued for a later step.

### 2.4 Staying Consistent

Whichever pattern you choose for a given process, use it **consistently across the whole model**. Mixing patterns within the same process. For example "In Review" followed by "Approving" followed by "Sent to Management" , makes the process harder to scan at a glance, because the user has to interpret each Task name individually instead of recognizing a familiar shape.

### 2.5 What to Avoid

- **Verbs or actions:** "Check Invoice" is an activity, not a state. A user reading this in a list cannot tell whether the checking is done or still to be done. Better: "In Review".
- **Vague, non-committal labels:** "Processing", "In Progress" say very little about what is actually happening. Where possible, name the specific state: "In Approval" instead of "In Progress".
- **Technical or internal terms:** Task names are shown to end users. Avoid internal jargon or system terminology that does not mean anything to the person working the task.

| Bad Task Name   | Better Task Name   | Why                                                 |
| --------------- | ------------------ | --------------------------------------------------- |
| "Check Invoice" | "In Review"        | Names an action, not a state                        |
| "Approving"     | "In Approval"      | Inconsistent with participle pattern used elsewhere |
| "Processing"    | "Pending Approval" | Too vague to be useful in a list                    |

## 3. Naming Events

An Event name should tell the user exactly what will happen if they click it. Since Events appear as action buttons directly on the form, the user reads them in the moment of decision. The name must work as a clear, unambiguous instruction, not as a description of the process.

A good Event name typically follows one of these patterns:

### 3.1 Imperative Verb

The most direct and common pattern: a single verb, addressing the user as a command.

Examples: _"Approve", "Reject", "Submit", "Archive"_

This is usually the best choice whenever the action is simple and self-explanatory. The user should be able to read the button and immediately know what pressing it does.

### 3.2 Imperative Verb + Object

When a single verb would be ambiguous, adding an object clarifies what exactly is being acted upon.

Examples: _"Request Info", "Forward to Management", "Send to Accounting"_

This pattern is typically used when a Task has several outgoing Events and the verb alone would not distinguish between them clearly (e.g. two different "Forward" options).

### 3.3 Staying Consistent

As with Task names, pick a grammatical form and apply it consistently across the model. Mixing an imperative verb ("Approve") with a noun-like label ("Approval") or a passive form ("Request Sent") forces the user to switch mental modes from button to button.

### 3.4 What to Avoid

- **States instead of actions:** "Approved" describes a result, not something the user does. Better: "Approve".
- **Questions:** "Approve?" reads as uncertain rather than as a clear instruction. State the action, not a question.
- **Vague or generic verbs:** "Process", "Continue", "Next" do not tell the user what actually happens. Prefer a specific verb that names the real action being taken.
- **Overly long labels:** An Event is a button, not a sentence. Keep it short enough to be read at a glance.

| Bad Event Name | Better Event Name       | Why                                          |
| -------------- | ----------------------- | -------------------------------------------- |
| "Approved"     | "Approve"               | Names a result, not an action                |
| "Approve?"     | "Approve"               | Reads as a question, not an instruction      |
| "Continue"     | "Forward to Management" | Too generic to communicate the actual action |

## 4. Coupling Events to the Resulting State

In the ideal case, an Event name does not just describe an action in isolation, it also hints at the state the process instance will reach afterwards. This creates a natural, readable link between what the user does and what happens next.

Often this link is visible directly in the wording: the Event uses the verb form, and the following Task uses the matching participle or adjective.

| Event (Action) | Resulting Task (State) |
| -------------- | ---------------------- |
| "Approve"      | "Approved"             |
| "Archive"      | "Archived"             |
| "Reject"       | "Rejected"             |

This coupling is valuable because it lets the user predict the outcome of an action before taking it, without reading documentation or asking a colleague. Clicking "Approve" and landing on "Approved" confirms that the system did exactly what was expected.

This does not mean the resulting Task name must always be the literal participle of the Event verb, that only works when the Event leads to a single, clear-cut outcome. In many models, the Task name will use a different but still clearly related term, for example because the target Task represents a broader or subsequent phase rather than the direct result of the Event itself.

| Event (Action)          | Resulting Task (State)        |
| ----------------------- | ----------------------------- |
| "Submit"                | "In Review"                   |
| "Forward to Management" | "Pending Management Approval" |
| "Request Info"          | "In Clarification"            |

What matters is not that the words match exactly, but that a user who reads the Event name can reasonably anticipate what kind of state comes next — not the exact process logic, but the general direction: is this moving forward, going back, or coming to a close?

This coupling works well as long as an Event leads to one predictable outcome. As you will see in the next chapter, this assumption breaks down for Events that route a process instance through several possible target Tasks depending on conditions, a case that needs a different naming logic of its own.

## 5. Edge Case: Routing and Correction Events

Not every Event fits the simple "Action → resulting State" logic described in the previous chapters. Some Events exist to correct or redirect a process instance rather than to move it forward in a predictable way. This is common in real-world models, and it is where naming tends to become difficult.

A typical scenario: a process instance is being processed, and a user needs to change something that affects which path the process instance should take, not just its content. The Event that triggers this change may pass through several intermediate Events or Gateways before the process instance reaches its actual target Task. In this situation, there is no single "resulting state" the Event name could point to.

**Example:** In an invoice approval process, a user handling payment release discovers that the payment method needs to change after the fact. Depending on the new payment method, the process instance must be routed to one of several different Tasks, for example "Domestic Payment Approval", "Foreign Payment Approval", or "Credit Note Offsetting". A single Event cannot be named after all of these possible outcomes at once.

In this case, the Event should be named after the reason or trigger for the correction, not after the (variable) target state:

- **"Change Payment Method"** - names the reason, works regardless of which Task the process instance ends up in.
- _"Forward to Domestic Payment"_ - would only be correct for one of several possible outcomes, and misleading for the others.

This is a legitimate, separate naming pattern, not a violation of the principles from Chapter 3. It applies whenever:

- an Event can lead to more than one possible target Task, depending on a condition or Gateway, **and**
- the target Tasks are not closely related enough to be summarized by one shared term.

**Rule of thumb:** If you cannot find one short, honest word for "where this leads," name the Event after **why** it happens instead of **where** it leads. The user will still understand the action; the routing itself is handled by the model, not by the label.

## 6. Checklist

Use this checklist as a quick reference while modelling — or to double-check a name you are unsure about.

**Naming a Task?**

- [ ] Does it complete the sentence "The process is currently ..."?
- [ ] Does it follow one consistent pattern: "In + Noun", Past Participle/Adjective, or Waiting State?
- [ ] Is it understandable on its own, without the workflow group or form open?
- [ ] Is it free of verbs, vague labels ("Processing"), and internal jargon?

**Naming an Event?**

- [ ] Does it complete the sentence "Now: ...", as an instruction to the user?
- [ ] Is it an imperative verb, optionally with an object ("Approve", "Forward to Management")?
- [ ] Is it free of states ("Approved"), questions ("Approve?"), and generic verbs ("Continue")?

**Does the Event lead to one clear, predictable Task?**

- [ ] Yes → name the Event after the action; make sure the resulting Task name is easy to anticipate (Chapter 4).
- [ ] No, it can route to several different Tasks depending on a condition → name the Event after the reason, not the target (Chapter 5).

**One last gut check:** If you had to copy a Task or Event name from a different process because you could not think of your own - stop, and ask again: is this a state, or is this an action, and what does it actually mean here?
