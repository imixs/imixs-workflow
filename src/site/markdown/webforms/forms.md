# Forms Layout

With [Imixs-Forms](https://github.com/imixs/imixs-forms) you can create forms completely model-based. The Imixs-Forms library enables you to design your forms at runtime. This is also named a 'low-code-development'. The definition of a form is done within the Imixs-BPMN modeler.

1. Create a Data-Object in your BPMN Model
2. Connect a task element with an association with your Data Object
3. Open the properties panel of the Data Object by double-click

<img class="screenshot" src="../images/imixs-forms-03.png" />

## Form Layout

Imixs Forms uses a flexible layout system based on sections and items. Each form definition must contain at least one section which is used to group form items.

### Basic Structure

A form definition always starts with the root element `imixs-form` followed by one or more sections:

```xml
<?xml version="1.0"?>
<imixs-form>
  <imixs-form-section label="Address Data:">
    <item name="zip" type="text"  label="ZIP:" span="2" />
    <item name="city" type="text"  label="City:" span="6" />
    <item name="country" type="text"  label="Country:" span="4" />
  </imixs-form-section>
</imixs-form>
```

### Sections

Sections are used to group related form items. Each section can have an optional label:

```xml
<imixs-form-section label="Order Details">
  ...items...
</imixs-form-section>
```

#### Grid Layout

Each section uses a 12-column grid system. You can define how many columns an item spans using the span attribute. For example:

- `span="12"`: Full width (default)
- `span="6"`: Half width
- `span="4"`: One third
- `span="3"`: One quarter
- `span="2"`: One sixth

Example of different column layouts:

```xml
<imixs-form-section label="Contact Information">
  <!-- Two columns of equal width -->
  <item name="firstname" type="text" label="First Name:" span="6" />
  <item name="lastname" type="text" label="Last Name:" span="6" />

  <!-- Three columns -->
  <item name="street" type="text" label="Street:" span="4" />
  <item name="city" type="text" label="City:" span="4" />
  <item name="zip" type="text" label="ZIP:" span="4" />

  <!-- Mixed column widths -->
  <item name="phone" type="text" label="Phone:" span="3" />
  <item name="email" type="text" label="Email:" span="9" />
</imixs-form-section>
```

### Form Items

Within a `imixs-form-section` you can define input fields called 'items' defined as `<item>` tags.
Each item has several attributes to control its appearance and behavior:

```xml
<item
  name="budget"          <!-- Item name (required) -->
  type="currency"        <!-- Input type (required) -->
  label="Budget:"        <!-- Label text (optional) -->
  span="6"               <!-- Column span (optional, default=12) -->
/>
```

The default width of an item is defined by the number of columns of the containing section. In addition each item has the following properties:

| Property | Type   | Mandatory | Description                               |
| -------- | ------ | --------- | ----------------------------------------- |
| name     | text   | x         | Name of the item                          |
| type     | text   | x         | Item type (e.g. text, currency, date,...) |
| label    | text   |           | Optional label for the Input field        |
| span     | number |           | Column span (optional, default=12)        |

## Input Fields & Item Names

Even if you can define the item names of your input fields in your custom form free, it is recommended to use a naming concept. This allows you to reuse code in a more easy way. The Imixs-Workflow project defines already a set of standard item names used for different business objects. This naming convention makes it more easy to group related items and to exchange data with your business process architecture.

The following sections list the business items typical used.
For application specific item names the ‘dot.Case’ format is recommended. It’s basically a convention that makes it easier to see what properties are related.

| Item             | Type  | Description                                 |
| ---------------- | ----- | ------------------------------------------- |
| **Order**        |       |                                             |
| order.name       | text  | Order name                                  |
| order.number     | text  | Order number                                |
| order.delivery   | date  | Delivery date                               |
| **Contract**     |       |                                             |
| contract.name    | text  | Contract name                               |
| contract.partner | text  | Contract partner name                       |
| contract.number  | text  | Contract number                             |
| contract.start   | date  | Contract start date                         |
| contract.end     | date  | Contract end date                           |
| contract.fee     | float | Contract fee per billing cycle              |
| **Creditor**     |       |                                             |
| cdtr.name        | text  | Creditor name                               |
| cdtr.iban        | text  | IBAN number                                 |
| cdtr.bic         | text  | BIC number                                  |
| **Debitor**      |       |                                             |
| dbtr.name        | text  | debitor name                                |
| dbtr.iban        | text  | IBAN number                                 |
| dbtr.bic         | text  | BIC number                                  |
| **Invoice**      |       |                                             |
| invoice.number   | text  | Invoice number                              |
| invoice.date     | date  | Invoice Date                                |
| invoice.total    | float | Invoice total amount                        |
| invoice.vat      | float | Invoice vat                                 |
| invoice.gross    | float | Invoice gross amount                        |
| invoice.currency | text  | currency                                    |
| **Payment**      |       |                                             |
| payment.type     | text  | credit card, SEPA                           |
| payment.date     | date  | payment date                                |
| payment.amount   | float | payment amount                              |
| payment.currency | text  | currency                                    |
| payment.cycle    | text  | payment cycle (monthly, yearly, fixed date) |

There are various input elements defined which can be used. See the section [form input parts](parts.html) for more details.

## Input Fields

Within a Imixs-Forms you can define different type of input parts within a `imixs-form-section`:

```xml
<imixs-form-section label="Controlling">
    <item name="name" type="text" label="Name" />
    <item name="description" type="textarea" label="Short Description" />
</imixs-form-section>
```

The following Input elements are currently defined:

### Text Input

```xml
<item name="description" type="text"
        label="Topic" />
```

### Textlist

A textlist is displayed as a input textarea. The entries of separate lines are stored as multiple values in an Item List. This input type can be useful e.g. for lists of E-Mail addresses or a list of order-numbers.

```xml
<item name="references" type="textlist"
        label="Order References" />
```

### Textarea Input

```xml
<item name="description" type="textarea"
        label="Description" />
```

### HTML/RichText Input

```xml
<item name="description" type="html"
        label="Description" />
```

### Date Input

```xml
<item name="invoice.date" type="date"
        label="Date" />
```

### Currency Input

```xml
<item name="invoice.amount" type="currency"
        label="Amount" />
```

### Select Boxes

You can also create different type of select boxes with predefined values:

```xml
    <item name="invoice.currency" type="selectOneMenu"
    label="Currency:"
    options="EUR;CHF;SEK;NOK;GBP;USD" />
```

You can choose one of the following types for select boxes:

- _selectOneMenu_ - a dropdown menu
- _selectBooleanCheckbox_ - a single checkbox
- _selectManyCheckbox_ - a list of checkboxes (layout=line direction)
- _selectOneRadio_ - radio buttons (layout=line direction)

_selectManyCheckbox_ and _selectOneRadio_ are displayed in line direction per default. If you want to display them in page direction use:

- _selectManyCheckboxPageDirection_ - a list of checkboxes (layout=page direction)
- _selectOneRadioPageDirection_ - radio buttons (layout=page direction)

You can also add a mapping of the name displayed in the select box and an optional value by using the '|' char:

```xml
<item name"myfield" type="selectOneMenu" required="true" label="Your Choice"
    options="management.it|Option A;management.backoffice|Option B" />
```
