
{% if title %}
# {{ title }}
{% endif %}

{% if description %}
{{ description }}
{% endif %}

{% if resource %}
{{ handler.resource }}
{% endif %}

{% for handler in handlers %}

## {{ handler.method }}

{{ handler.title }}

{{ handler.description }}

{% if handler.resource %}
{{ handler.resource }}
{% endif %}

{% if handler.spec-in %}
<details>
<summary>Intput schema</summary>

~~~json
{{ handler.spec-in|json-pretty|safe }}
~~~

</details>
{% endif %}

{% if handler.spec-out %}
<details>
<summary>Output schema</summary>

~~~json
{{ handler.spec-out|json-pretty|safe }}
~~~

</details>
{% endif %}

{% endfor %}
