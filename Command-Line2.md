# dict CLI Reference

**dict** is a powerful command-line tool that transforms structured data into beautiful diagrams. This comprehensive guide covers everything from quick starts to advanced workflows.

## Table of Contents

- [Installation](#installation)
- [Quick Start](#quick-start)
- [Core Workflows](#core-workflows)
- [Command Reference](#command-reference)
- [Advanced Usage](#advanced-usage)
- [Tips & Troubleshooting](#tips--troubleshooting)

## Installation

### Native Binaries (Recommended)

**macOS (Homebrew)**
```bash
brew install judepayne/tap/dictim
dict --help
```

**Windows (Scoop)**
```bash
scoop bucket add judepayne https://github.com/judepayne/scoop-judepayne
scoop install dictim
dict --help
```

**Linux/Manual Installation**
```bash
# Download latest release (amd64)
wget https://github.com/judepayne/dictim/releases/latest/download/dict-linux-amd64
chmod +x dict-linux-amd64
sudo mv dict-linux-amd64 /usr/local/bin/dict

# Or ARM64
wget https://github.com/judepayne/dictim/releases/latest/download/dict-linux-arm64
chmod +x dict-linux-arm64
sudo mv dict-linux-arm64 /usr/local/bin/dict
```

### Universal Babashka Script

**Prerequisites:** Install [babashka](https://babashka.org) and [bbin](https://github.com/babashka/bbin)

```bash
bbin install https://github.com/judepayne/dictim/releases/latest/download/dictim.jar
dict -h
```

> **Note:** If `dict` command is not found, add `~/.local/bin` to your PATH

## Quick Start

### Your First Diagram

**Create data directly on command line:**
```bash
dict -i '["app" "Web App"]["db" "Database"]["app" "->" "db" "queries"]' > first.svg
```

**From a file:**
```bash
echo '["user" "User" {"shape" "person"}]
      ["system" "System"] 
      ["user" "->" "system" "interacts"]' > simple.edn

dict -i simple.edn -o simple.svg
```

**Live development:**
```bash
# Watch and serve in browser with live-reloading diagram
dict -iw simple.edn

# Or watch and compile to SVG directly
dict -iw simple.edn -o live.svg    # Updates SVG file on changes
```

## Core Workflows

### Creating Diagrams from Data

The most common workflow: transform your structured data into visual diagrams.

#### Direct SVG Generation
Transform data directly to SVG diagrams without intermediate steps:

```bash
# Simple architecture
dict -i '["frontend" "React App"]
         ["api" "REST API"] 
         ["db" "PostgreSQL"]
         ["frontend" "->" "api"]
         ["api" "->" "db"]' -o architecture.svg

# With styling
dict -i '["user" "User" {"shape" "person"}]
         ["app" "Application" {"shape" "hexagon"}]
         ["user" "->" "app" "uses" {"style.stroke" "blue"}]' -o styled.svg

# Apply themes
dict -i --theme 3 < data.json -o themed.svg
```

#### Two-Step Process
For more control or integration with d2 ecosystem:

```bash
# Generate D2 code
dict -c < data.json > diagram.d2

# Render with specific d2 options
dict -c < data.json | d2 --layout elk --theme 4 - final.svg
```

### Converting Existing D2

Migrate existing D2 diagrams to structured data for version control and programmatic manipulation:

**existing.d2**
```d2
frontend: React App {shape: rectangle}
api: REST API {shape: hexagon}
database: PostgreSQL {shape: cylinder}

frontend -> api: HTTP
api -> database: SQL
```

**Convert to structured data:**
```bash
# To EDN format
dict -p existing.d2 > structured.edn

# To pretty JSON
dict -p -j -m existing.d2 > structured.json
```

**Result: structured.edn**
```edn
(["frontend" "React App" {"shape" "rectangle"}]
 ["api" "REST API" {"shape" "hexagon"}]
 ["database" "PostgreSQL" {"shape" "cylinder"}]
 ["frontend" "->" "api" "HTTP"]
 ["api" "->" "database" "SQL"])
```

### Styling with Templates

Apply consistent styling across multiple diagrams:

**Create a corporate template (corporate.edn):**
```edn
{:template
 [["matches" "shape" "person"] {:style.fill "'#e1f5fe'"}
  ["matches" "shape" "hexagon"] {:style.fill "'#f3e5f5'"}
  ["matches" "label" ".*API.*"] {:style.stroke "'#1976d2'"}]
 :directives
 {:direction "right"
  :classes
  {"primary" {:style {:fill "'#2196f3'" :stroke "'#1565c0'"}}
   "secondary" {:style {:fill "'#ff9800'" :stroke "'#ef6c00'"}}}}}
```

**Apply to diagrams:**
```bash
# Direct to SVG
dict -i -t corporate.edn < system-data.json -o branded.svg

# Or apply to existing D2
dict -a -t corporate.edn existing.d2 | d2 - styled.svg
```

### Working with Graphspecs

Graphspecs are dictim's high-level format for expressing complex systems as structured data. Unlike regular dictim data, graphspecs preserve the full richness of your original data for longer in processing pipeline, so that you can set up styling rules with templates (for example):

**Why use graphspecs?**

Graphspecs work on complete data representations for nodes and edges, whereas converting directly to dictim often loses information during the transformation. This richer data model enables powerful processing features:

- **Node templates** can access all node properties for conditional styling
- **Edge templates** ditto
- **Container mapping** can organize nodes into clusters based on any data field
- **Better separation** between your data structure and its visual representation

Graphspecs are particularly valuable when you're generating diagrams programmatically from databases, APIs, or other data sources that will be processed repeatedly.

**Example graphspec (system.json):**
```json
{
  "nodes": [
    {"id": "app1", "name": "Web Service", "dept": "Engineering", "cost": 15000},
    {"id": "app2", "name": "Database", "dept": "Infrastructure", "cost": 8000},
    {"id": "app3", "name": "Cache", "dept": "Infrastructure", "cost": 3000}
  ],
  "edges": [
    {"src": "app1", "dest": "app2", "data-type": "SQL queries", "frequency": "high"},
    {"src": "app1", "dest": "app3", "data-type": "cache requests", "frequency": "medium"}
  ],
  "node->key": "id",
  "node->container": "dept",
  "node-template": [
    [">", "cost", 10000], {"style.fill": "'red'"},
    ["=", "dept", "Engineering"], {"style.fill": "'blue'"}
  ],
  "edge-template": [
    ["=", "frequency", "high"], {"style.stroke-width": "3"}
  ]
}
```

**Convert to dictim and render:**
```bash
# Process graphspec to structured dictim
dict -g < system.json -o system.edn

# Render to diagram
dict -i < system.edn -o system.svg

# Or combine with templates
dict -g -t corporate.edn < system.json -o styled-system.edn
```

**With watch mode for development:**
```bash
# Watch graphspec and template files
dict -gw system.json -t corporate.edn -o system.edn
```

This workflow excels when building dashboards, documentation systems, or any scenario where diagrams are generated from live data sources.

### Live Development with Watch Mode

Perfect for iterative diagram development:

**Watch a file and serve in browser:**
```bash
# Image watch mode opens browser with live-reloading diagram
dict -iw architecture.edn
# Edit architecture.edn and see changes instantly
```

**Watch with output file:**
```bash
# Continuously update D2 file
dict -cw source.edn -o output.d2
# Or using separate flags:
dict -c -w source.edn -o output.d2

# Watch with template
dict -cw source.edn -t styles.edn -o output.d2
```

**Watch with d2 options (image mode):**
```bash
dict -iw diagram.edn --layout elk --theme 2 --scale 1.5
```

## Command Reference

### Core Commands

| Command | Alias | Description | Example |
|---------|-------|-------------|---------|
| `--compile` | `-c` | Transform dictim data to D2 | `dict -c < data.json` |
| `--image` | `-i` | Transform data directly to SVG | `dict -i < data.json -o diagram.svg` |
| `--parse` | `-p` | Convert D2 to structured data | `dict -p < diagram.d2` |
| `--apply-tmp` | `-a` | Apply template to D2 | `dict -a -t style.edn < diagram.d2` |


### Bundled Options


| Command | Description | Equivalent | Example |
|---------|-------------|------------|---------|
| `-cw` | Shorthand for `-c -w` (compile and watch) | `-c -w` | `dict -cw data.edn -o output.d2` |
| `-pw` | Shorthand for `-p -w` (parse and watch) | `-p -w` | `dict -pw diagram.d2 -o output.edn` |
| `-iw` | Shorthand for `-i -w` (image and watch) | `-i -w` | `dict -iw data.edn -o output.svg # (the -o can be ommitted)`|
| `-aw` | Shorthand for `-a -w` (apply template and watch) | `-a -w` | `dict -aw diagram.d2 -t styles.edn -o output.d2` |
| `-gw` | Shorthand for `-g -w` (graph spec and watch) | `-g -w` | `dict -gw graphspec.json -t styles.edn -o output.edn` |

> **Note:** For `-iw`, the `-o` flag can be omitted. When omitted, an SVG file with the same name as the input file will be created.

### Data Manipulation

| Command | Alias | Description | Example |
|---------|-------|-------------|---------|
| `--graph` | `-g` | Convert graph spec to dictim | `dict -g < graph-spec.json` |
| `--flatten` | `-f` | Convert to flat dictim syntax | `dict -f < data.edn` |
| `--build` | `-b` | Build from flat dictim syntax | `dict -b < flat-data.edn` |
| `--validate` | `-val` | Validate dictim syntax | `dict -val < data.edn` |

### Formatting Options

| Command | Alias | Description | Example |
|---------|-------|-------------|---------|
| `--keywordize` | `-k` | Convert keys to keywords (EDN) | `dict -k -p < diagram.d2` |
| `--stringify` | `-st` | Convert keys to strings | `dict -st < data.edn` |
| `-j` | | Output as JSON | `dict -p -j < diagram.d2` |
| `-m` | | Pretty-print JSON | `dict -p -j -m < diagram.d2` |
| `-r` | | Remove styles from parsed D2 | `dict -p -r < styled.d2` |

### D2 Integration

| Command | Alias | Description | Values | Example |
|---------|-------|-------------|--------|---------|
| `--layout` | `-l` | D2 layout engine | `dagre`, `elk`, `tala` | `dict -i --layout elk < data.json` |
| `--theme` | `-th` | D2 theme ID | `0-303` | `dict -i --theme 4 < data.json` |
| `--scale` | `-s` | SVG scaling factor | `0.1-10.0` | `dict -i --scale 1.5 < data.json` |
| `-d` | | Debug mode (shows D2 in browser) | | `dict -w -d data.edn` |

### File Operations

| Command | Alias | Description | Example |
|---------|-------|-------------|---------|
| `--output` | `-o` | Specify output file | `dict -i < data.json -o diagram.svg` |
| `--template` | `-t` | Apply template file | `dict -i -t styles.edn < data.json` |

### System

| Command | Alias | Description |
|---------|-------|-------------|
| `--help` | `-h` | Show help information |
| `--version` | `-v` | Show version |

## Advanced Usage

### Complex Data Pipelines

**From Kubernetes to Diagram:**
```bash
kubectl get services -o json | \
  jq -r '.items | map([.metadata.name, .spec.type]) | @json' | \
  dict -i --theme 3 --layout elk -o k8s-services.svg
```

**Git Branch Visualization:**
```bash
git branch -r | \
  bb git-transform.bb | \
  dict -i -o branches.svg
```

### Combining Multiple Operations

**Parse, modify, and regenerate:**
```bash
# Parse existing D2
dict -p existing.d2 > data.edn

# Edit data.edn programmatically or manually

# Regenerate with new styling
dict -i -t corporate.edn < data.edn -o updated.svg
```

**Batch processing:**
```bash
for file in diagrams/*.d2; do
  dict -a -t standard.edn "$file" | d2 - "output/$(basename "$file" .d2).svg"
done
```

### Watch Mode Advanced Features

**Multiple file watching:**
```bash
# Watch both data and template files
dict -cw data.edn -t styles.edn -o output.d2
# Or using separate flags:
dict -c -w data.edn -t styles.edn -o output.d2
```

**Custom d2 options (image mode):**
```bash
dict -iw architecture.edn \
  --layout tala \
  --theme 105 \
  --scale 2.0 \
  -d  # Show D2 code in browser for debugging
```

### Template Development

**Incremental template development:**
```bash
# Terminal 1: Watch template application
dict -aw sample.d2 -t styles.edn -o styled.d2
# Or using separate flags:
dict -a -w sample.d2 -t styles.edn -o styled.d2

# Terminal 2: Watch final output (image mode)
dict -iw styled.d2
```

### Data Format Conversion

**EDN to JSON workflow:**
```bash
# Convert EDN to JSON for sharing
dict -p -j -m data.edn > data.json

# Validate converted data
dict -val < data.json
```

**Clean up parsed D2:**
```bash
# Remove styling to get clean structure
dict -p -r -k styled-diagram.d2 > clean-structure.edn
```

### Integration Examples

**CI/CD Pipeline:**
```yaml
# GitHub Actions example
- name: Generate Architecture Diagrams
  run: |
    dict -i -t corporate.edn < infrastructure.json -o docs/architecture.svg
    dict -i --theme 2 < database-schema.edn -o docs/database.svg
    git add docs/*.svg
```

**NPM Scripts:**
```json
{
  "scripts": {
    "docs:diagrams": "dict -i -t styles.edn < architecture.json -o docs/arch.svg",
    "docs:watch": "dict -iw architecture.edn",
    "docs:validate": "dict -val *.edn"
  }
}
```

## Tips & Troubleshooting

### Performance Tips

**Large diagrams:**
```bash
# Use elk layout for complex diagrams
dict -i --layout elk < large-architecture.json -o output.svg

# Increase scale for better readability
dict -i --scale 1.5 < dense-diagram.edn -o readable.svg
```

**Batch processing:**
```bash
# Process multiple files efficiently
find . -name "*.edn" -exec dict -i < {} -o {}.svg \;
```

### Common Issues

**Windows Command Line:**
```bash
# Use double quotes instead of single quotes
dict -i "[\"app\", \"Web App\"]" -o diagram.svg

# Escape special characters with ^
dict -i "[\"user\", \"-^>\", \"app\"]" -o flow.svg

# Better: use files instead of command-line strings
dict -i < data.json -o diagram.svg
```

**File Path Issues:**
```bash
# Use absolute paths for templates
dict -i -t /full/path/to/styles.edn < data.json -o output.svg

# Or ensure working directory is correct
cd project-root
dict -i -t ./templates/styles.edn < data.json -o output.svg
```

### Debug Workflows

**Check data format:**
```bash
# Validate your data
dict -val < data.edn

# Parse and reformat to check structure
dict -p -j -m diagram.d2
```

**Validation errors:**
```bash
# Templates and graphspecs are validated in detail when they are used:
dict -g < invalid-graphspec.json  # Shows specific validation failures
dict -i -t invalid-template.edn < data.json  # Shows template validation errors
```

You can validate dictim data before processing it:

```bash
# Validate data before processing
dict -val < data.edn
```

**Template debugging:**
```bash
# See intermediate D2 with debug mode (image watch)
dict -iw -d -t styles.edn data.edn

# Apply template and check D2 output
dict -a -t styles.edn data.edn
```

**D2 integration issues:**
```bash
# Check if d2 is available
which d2

# Test d2 directly
echo "a -> b" | d2 - test.svg

# Use dict with explicit d2 options
dict -c data.edn | d2 --layout elk --theme 4 - output.svg
```

### Best Practices

1. **Use files over command-line strings** for complex data
2. **Validate data** before processing: `dict -val < data.edn`
3. **Use watch mode** for iterative development: `dict -iw data.edn`
4. **Combine with templates** for consistent styling across projects
5. **Version control** your dictim data files for diagram history
6. **Use meaningful filenames** that describe the diagram content
7. **Test with different layouts** (`dagre`, `elk`, `tala`) to find best fit
8. **Leverage themes** for professional appearance without custom CSS

### Getting Help

```bash
# Show all available commands
dict --help

# Get detailed help for specific functionality
dict -c --help
dict -p --help
dict -i --help
```

For more examples and patterns, visit the [dictim cookbook](https://github.com/judepayne/dictim.cookbook) and [wiki](https://github.com/judepayne/dictim/wiki).
