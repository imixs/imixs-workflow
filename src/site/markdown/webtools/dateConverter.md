# Imixs DateConverter

With the ImixsDateConverter it is possible to convert dateTime values. In differnt to the `f:convertDateTime` this converter also accepts empty values:


```xml
	<h:inputText value="#{workitem.item[item.name]}" >
		<f:converter converterId="ImixsDateConverter" />
		<f:attribute name="org.imixs.date.pattern" value="dd.MM.yyyy" />
		<f:attribute name="org.imixs.date.timeZone" value="CET" />
	</h:inputText>
```

If no pattern and timeZone attributes are provided the default values are


    org.imixs.date.pattern = "yyyy-MM-dd"
    org.imixs.date.timeZone = "UTC"