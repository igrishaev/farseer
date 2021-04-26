
# {{ title }}

{{ description }}

{# {{ file }} #}

{# {{ file }} #}


{% for handler in handlers %}

## {{ handler.method }}

{{ handler.title }}

{{ handler.description }}

Intput schema:

```json
{{ handler.spec-in|json-pp|safe }}
```

Output schema:

```json
{{ handler.spec-out|json-pp|safe }}
```

{% endfor %}
