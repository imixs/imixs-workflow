# Form Input Types

This page is the reference for all standard input types defined by the
[Imixs Form XML Specification](forms.html).
Every conforming implementation must support these types.

An input field is defined as an `<item>` element inside an `<imixs-form-section>`:

```xml
<imixs-form-section label="Invoice">
  <item name="invoice.number" type="text"     label="Invoice Number:" span="6" />
  <item name="invoice.date"   type="date"     label="Invoice Date:"   span="6" />
  <item name="invoice.total"  type="currency" label="Total Amount:"   />
</imixs-form-section>
```

The `type` attribute determines which input component is rendered.
The `name` attribute maps the field value directly to a named item in the Imixs workitem.

---

## Text Input

A single-line text input for short string values.

```xml
<item name="order.number" type="text" label="Order Number:" />
```

---

## Textarea

A multi-line text input for longer free-form text.

The optional `options` attribute accepts inline CSS styles to control the height:

```xml
<item name="order.notes" type="textarea" label="Notes:" />

<!-- with custom height -->
<item name="order.notes" type="textarea" label="Notes:" options="height: 15em;" />
```

---

## Text List

A multi-line input where each line is stored as a **separate value** in the workitem item list.
Useful for lists of e-mail addresses, order references, tag lists, or any other
multi-value data.

```xml
<item name="order.references" type="textlist" label="Order References:" />
```

The values are stored as a multi-value item, not as a single newline-delimited string.

---

## HTML / Rich Text

A WYSIWYG rich text editor. The value is stored as an HTML string.

```xml
<item name="contract.description" type="html" label="Description:" />
```

---

## Date

A date picker. The value is stored as a Java `Date` object in the workitem.

```xml
<item name="invoice.date" type="date" label="Invoice Date:" />
```

---

## HTML5 Date

A native HTML5 date picker rendered by the browser. Compared to `date`, this type relies
entirely on the browser's built-in date input (`<input type="date">`) without any additional
JavaScript widget. Browser support and visual appearance may vary.
The value is stored as a Java `Date` object in the workitem.

```xml
<item name="invoice.date" type="html5date" label="Invoice Date:" />
```

---

## HTML5 DateTime

A native HTML5 date and time picker rendered by the browser (`<input type="datetime-local">`).
Use this type when both a date and a time of day need to be captured in a single field.
The value is stored as a Java `Date` object in the workitem.

```xml
<item name="meeting.start" type="html5datetime" label="Meeting Start:" />
```

---

## Currency

A decimal number input formatted as a currency value.
The value is stored as a Java `Double` in the workitem. Formatting (symbol, decimal separator,
thousands separator) is handled by the rendering implementation.

```xml
<item name="invoice.total" type="currency" label="Total Amount:" />
```

---

## Select Boxes

Select inputs allow the user to choose from a predefined list of options.
Options are defined in the `options` attribute as a **semicolon-separated list**.

An optional display label can be separated from the stored value using the `|` character:

```xml
<!-- stored value equals displayed label -->
<item name="invoice.currency" type="selectOneMenu" label="Currency:"
      options="EUR;CHF;GBP;USD" />

<!-- stored value differs from displayed label -->
<item name="department" type="selectOneMenu" label="Department:"
      options="it|IT Department;hr|Human Resources;fin|Finance" />
```

The following select types are available:

### Dropdown Menu

Renders a standard dropdown. The user can select exactly one value.

```xml
<item name="invoice.currency" type="selectOneMenu" label="Currency:"
      options="EUR;CHF;GBP;USD" />
```

### Single Checkbox

Renders a single checkbox. The stored value is boolean (`true` / `false`).

```xml
<item name="order.confirmed" type="selectBooleanCheckbox" label="Order confirmed" />
```

### Multiple Checkboxes

Renders a list of checkboxes. The user can select multiple values.
Items are arranged in **line direction** (horizontal) by default:

```xml
<item name="order.tags" type="selectManyCheckbox" label="Tags:"
      options="urgent;internal;reviewed" />
```

For **page direction** (vertical, one per line):

```xml
<item name="order.tags" type="selectManyCheckboxPageDirection" label="Tags:"
      options="urgent;internal;reviewed" />
```

### Radio Buttons

Renders a group of radio buttons. The user can select exactly one value.
Items are arranged in **line direction** (horizontal) by default:

```xml
<item name="contract.billing" type="selectOneRadio" label="Billing Cycle:"
      options="monthly;quarterly;yearly" />
```

For **page direction** (vertical, one per line):

```xml
<item name="contract.billing" type="selectOneRadioPageDirection" label="Billing Cycle:"
      options="monthly;quarterly;yearly" />
```

---

## Summary Table

| Type                              | Java Type      | Selection | Layout                              |
| --------------------------------- | -------------- | --------- | ----------------------------------- |
| `text`                            | `String`       | —         | single line                         |
| `textarea`                        | `String`       | —         | multi-line                          |
| `textlist`                        | `List<String>` | —         | multi-value                         |
| `html`                            | `String`       | —         | rich text                           |
| `date`                            | `Date`         | —         | date picker (widget)                |
| `html5date`                       | `Date`         | —         | date picker (native browser)        |
| `html5datetime`                   | `Date`         | —         | date + time picker (native browser) |
| `currency`                        | `Double`       | —         | decimal                             |
| `selectOneMenu`                   | `String`       | single    | dropdown                            |
| `selectBooleanCheckbox`           | `Boolean`      | boolean   | checkbox                            |
| `selectManyCheckbox`              | `List<String>` | multiple  | horizontal                          |
| `selectManyCheckboxPageDirection` | `List<String>` | multiple  | vertical                            |
| `selectOneRadio`                  | `String`       | single    | horizontal                          |
| `selectOneRadioPageDirection`     | `String`       | single    | vertical                            |

---

## Custom Input Types

Applications built on Imixs-Workflow can define their own input types beyond this standard set.
See [Custom Input Types](input-custom-types.html) for details on how to implement and
register custom components.
