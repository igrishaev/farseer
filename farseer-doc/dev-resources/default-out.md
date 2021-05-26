

# My API



Long API Description






## user/delete

Delete a user by ID

Long text for deleting a user.




<details>
<summary>Intput schema</summary>
```json
{
  "type" : "integer",
  "format" : "int64",
  "minimum" : 1
}
```
</details>



<details>
<summary>Output schema</summary>
```json
{
  "type" : "object",
  "properties" : {
    "message" : {
      "type" : "string"
    }
  },
  "required" : [ "message" ]
}
```
</details>




## user/get-by-id

Get a user by ID

Long text for getting a user.



### Get user by ID examples

(this text comes from a separate file)

```bash
FOO=42 do this
```

- one example
- another example
- test

[link]: test.com

Check this [link][link] for more info.




<details>
<summary>Intput schema</summary>
```json
{
  "type" : "integer",
  "format" : "int64"
}
```
</details>



<details>
<summary>Output schema</summary>
```json
{
  "type" : "object",
  "additionalProperties" : {
    "anyOf" : [ {
      "type" : "integer",
      "format" : "int64"
    }, {
      "type" : "string"
    } ]
  }
}
```
</details>



