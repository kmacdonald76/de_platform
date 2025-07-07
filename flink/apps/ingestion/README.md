# Flink Ingestion - JSON Flattening Configuration

This document describes the YAML-based configuration for flattening complex, nested JSON structures into a flat, row-based format suitable for Flink ingestion. This configuration is designed to be part of the data source's secret, providing instructions on how to transform the raw JSON payload.

The flattening process applies transformations from the "inside out," progressively merging data into a flattened row.

## Core Concepts

The flattening configuration utilizes a functional approach, where specific functions (`f.map_merge_with_rows`, `f.array_of_maps_to_rows`) define how different JSON structures are processed and merged into rows.

### 1. `f.map_merge_with_rows`

This function is used to iterate over the keys of a JSON object and merge the key-value pairs into the flattened rows. It's ideal for scenarios where you have dynamic keys that you want to capture as a field in your output.

**Syntax:**

```yaml
f.map_merge_with_rows:
  <key_to_process_1>: <schema_or_nested_function>
  <key_to_process_2>: <schema_or_nested_function>
  ...
  f._parent_key: <string_type> # Optional: Captures the key of the current map entry
```

*   **`<key_to_process>`**: A direct child key of the current JSON object. The value associated with this key will be processed according to the provided schema or nested function.
*   **`f._parent_key`**: A special keyword. If present, the *key* of the current map entry being processed by `f.map_merge_with_rows` will be added as a field to the flattened row with the specified type.

### 2. `f.array_of_maps_to_rows`

This function is used to iterate over an array of JSON objects, where each object in the array should be converted into one or more flattened rows.

**Syntax:**

```yaml
f.array_of_maps_to_rows:
  <array_key>:
    <field_name_1>: <data_type>
    <field_name_2>: <data_type>
    ...
```

*   **`<array_key>`**: The key of the array within the current JSON object that contains the list of maps to be converted into rows.
*   **Nested Schema**: Directly under `<array_key>`, you define the schema for each object within that array. Each `<field_name>` corresponds to a key within the array's objects, and `<data_type>` specifies its type.

### Data Types

The following data types are supported for defining the schema of your flattened fields:

*   `string`
*   `integer`
*   `long`
*   `double`
*   `boolean`
*   `array<string>`
*   `array<integer>`
*   `array<long>`
*   `array<double>`
*   `array<boolean>`

## Example

Let's consider the following input JSON structure:

```json
{
    "category1": {
        "game_list": [
            {
                "name": "game1",
                "field1": "val1",
                "field2": "val2",
                "field3": ["listval1", "listval2"]
            },
            {
                "name": "game2",
                "field1": "val1",
                "field2": "val2",
                "field3": ["listval1", "listval2"]
            }
        ],
        "category_attributes": ["super", "cool"]
    },
    "category2": {
        "game_list": [
            {
                "name": "game3",
                "field1": "vala",
                "field2": "valb",
                "field3": ["listvala", "listvala"]
            },
            {
                "name": "game4",
                "field1": "vala",
                "field2": "valb",
                "field3": ["listvala", "listvala"]
            }
        ],
        "category_attributes": ["ultra", "cool"]
    }
}
```

Our goal is to flatten this into rows where each row represents a single game, and includes its associated category information.

The corresponding YAML flattening configuration would be:

```yaml
f.map_merge_with_rows: # Outer level: Iterate over "category1", "category2"
    f.map_merge_with_rows: # Middle level: Process content within each category (e.g., "game_list", "category_attributes")
        f.array_of_maps_to_rows: # Inner level: Convert each game in "game_list" array to rows
            game_list:
                name: string
                field1: string
                field2: string
                field3: array<string>
        category_attributes: array<string> # Direct field from the category object
    f._parent_key: string # Captures the category name (e.g., "category1")
```

**Explanation of the Example:**

1.  **Outermost `f.map_merge_with_rows`**: This targets the root of the JSON. It iterates over `category1` and `category2`. The `f._parent_key: string` at this level will capture "category1" and "category2" as a new field in the flattened output.
2.  **Middle `f.map_merge_with_rows`**: This operates on the *value* of each category (e.g., the object containing `game_list` and `category_attributes`). It processes `game_list` using `f.array_of_maps_to_rows` and directly extracts `category_attributes`.
3.  **Innermost `f.array_of_maps_to_rows`**: This targets the `game_list` array. For each object within `game_list`, it extracts `name`, `field1`, `field2`, and `field3` according to their specified types, forming the base of each flattened row.

**Expected Flattened Output (Conceptual):**

The flattening process will conceptually produce rows similar to this:

| f._parent_key (category) | category_attributes | name (game) | field1 | field2 | field3           |
| :----------------------- | :------------------ | :---------- | :----- | :----- | :--------------- |
| category1                | ["super", "cool"]   | game1       | val1   | val2   | ["listval1", "listval2"] |
| category1                | ["super", "cool"]   | game2       | val1   | val2   | ["listval1", "listval2"] |
| category2                | ["ultra", "cool"]   | game3       | vala   | valb   | ["listvala", "listvala"] |
| category2                | ["ultra", "cool"]   | game4       | vala   | valb   | ["listvala", "listvala"] |

## Implicit Pathing

It's important to note that the keys specified in the YAML configuration (e.g., `game_list`, `category_attributes`) are expected to be direct children of the current JSON object being processed by the respective `f.` function. There is no explicit JSONPath syntax within the schema definitions themselves; the structure of the YAML directly mirrors the expected structure of the JSON being flattened.
