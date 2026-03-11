# Custom Components

The [Imixs Form Specification](forms.html) defines a set of
[standard input types](forms-input-types.html) that every conforming implementation must support.
Beyond these, any application built on Imixs-Workflow can define and register its own
**custom components** — without modifying the BPMN model structure or the workflow engine itself.

This extensibility is by design. The specification defines two levels at which custom
components can be introduced:

- **Custom Item** — a single input field rendered by a custom component
- **Custom Section** — an entire form section rendered by a custom component

In both cases the workflow engine stores the XML as-is and plays no role in rendering.
The application resolves the custom component at runtime.

---

## Custom Items

A custom item replaces a single `<item>` with an application-specific input component.
It is defined by setting `type="custom"` and adding a `path` attribute that identifies
the component:

```xml
<imixs-form-section label="Request">
  <item name="request.text" type="custom" path="markdowneditor" label="Request:" />
</imixs-form-section>
```

### Attributes

| Attribute  | Type    | Mandatory | Description                                                 |
| ---------- | ------- | --------- | ----------------------------------------------------------- |
| `type`     | text    | ✓         | Must be `custom`                                            |
| `path`     | text    | ✓         | Identifies the custom component to be rendered              |
| `name`     | text    | ✓         | The item name in the workitem (same as for standard types)  |
| `label`    | text    |           | Optional visible label (same as for standard types)         |
| `readonly` | boolean |           | Optional, renders the component as read-only                |
| `options`  | text    |           | Optional configuration string, interpreted by the component |

The `path` value is application-specific. How it is resolved — whether as a file path,
a component name, a CDI bean identifier, or any other mechanism — depends entirely on the
rendering implementation.

### Responsibilities

A custom item component is responsible for:

1. **Rendering** an appropriate UI element for the given `name` and `path`
2. **Reading** the current value from the workitem item identified by `name`
3. **Writing** the user's input back to the workitem item identified by `name`

The data type stored in the workitem is defined by the custom component itself, not by
this specification.

### Example: Markdown Editor

```xml
<item name="request.body" type="custom" path="markdowneditor" label="Request:" />
```

### Example: Custom Component with Options

The `options` attribute can pass configuration to the component.
Its format and interpretation are entirely up to the component:

```xml
<item name="product.image" type="custom" path="imageupload"
      label="Product Image:" options="maxsize=2MB;accept=image/png,image/jpeg" />
```

---

## Custom Sections

A custom section replaces an entire `<imixs-form-section>` with an application-specific
component. This is useful for complex form areas that go beyond what individual items
can express — for example sections with conditional visibility, dynamic content,
or multiple interacting fields.

A custom section is defined by adding a `path` attribute directly to the
`<imixs-form-section>` element. It contains no `<item>` children:

```xml
<imixs-form-section label="References" path="sub_references_form" />
```

### Attributes

| Attribute  | Type    | Mandatory | Description                                                        |
| ---------- | ------- | --------- | ------------------------------------------------------------------ |
| `path`     | text    | ✓         | Identifies the custom section component to be rendered             |
| `label`    | text    |           | Optional visible heading for the section                           |
| `readonly` | boolean |           | Optional, signals to the component that it should render read-only |

### Responsibilities

A custom section component takes full control over its rendering area. It is responsible for:

1. **Rendering** the complete section UI for the given `path`
2. **Reading and writing** workitem data directly, using whatever item names it requires
3. **Respecting the `readonly` attribute** when provided

A custom section may implement any interaction pattern supported by the host application,
including dynamic updates and interdependent fields. The details of that interaction
are outside the scope of this specification and are defined by the application.

---

## Implementing Custom Components in Your Application

How custom components are implemented depends on the technology stack of the application.
The following projects provide documented extension mechanisms:

- **Imixs-Office-Workflow** — supports custom items and custom sections via Jakarta Faces
  composite components. See the
  [Custom Input Parts](https://doc.office-workflow.com/forms/parts-custom.html)
  documentation for a step-by-step guide.

- **Imixs-Forms (JavaScript)** — custom types can be registered as JavaScript modules
  at application startup. See the
  [Imixs-Forms repository](https://github.com/imixs/imixs-forms) for details.

If you are building your own application on top of Imixs-Workflow, you are free to define
your own resolution mechanism for the `path` attribute. The only contract this specification
defines is the XML structure of the `<item>` and `<imixs-form-section>` elements.

---

## Portability Considerations

Because custom components are application-specific, form definitions that use `type="custom"`
or a `path` on a section are not fully portable across different implementations.
When sharing BPMN models between applications, keep the following in mind:

- Standard types (`text`, `date`, `currency`, etc.) are always portable.
- Custom items and sections are only rendered correctly by applications that have the
  referenced component registered under the same `path` name.
- If a rendering application encounters an unknown `path`, it should degrade gracefully —
  for example by rendering a plain text input or skipping the section entirely.

This is not a limitation but a deliberate trade-off: it allows each application to build
rich, domain-specific form components while keeping the core specification lean and stable.
