# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Overview

dictim is a Clojure/ClojureScript library that provides nested EDN/JSON syntax for expressing diagrams as data. It compiles to d2 (diagram scripting language) and Graphviz's DOT format, and can parse d2 back into dictim format. The project supports both Clojure and Babashka.

## Commands

### Testing
```bash
# Run all tests
clojure -M:test

# Run specific test namespace  
clojure -M:test -n dictim.d2.parse-test
clojure -M:test -n dictim.d2.compile-test
```

### Development  
```bash
# REPL with test dependencies
clojure -M:test:repl

# Build native binary (requires GraalVM)
clojure -M:native -m cmd.dictim --help
```

## Architecture

### Core Components

**dictim.d2.parse** - Contains the d2 parser built with Instaparse. The grammar is dynamically generated using literal definitions and regex builders to support d2's complex syntax including containers, connections, attributes, vars, classes, and connection references.

**dictim.d2.compile** - Compiles dictim format back to d2 using multimethod dispatch on element types (:shape, :ctr, :conn, :attrs, :vars, :classes, etc.). Uses dynamic binding for compilation context and formatting.

**dictim.d2.attributes** - Defines all d2 reserved keywords, shapes, and validation rules. Handles context validation (e.g., style attributes must be under 'style' key) and attribute value validation.

**dictim.validate** - Validates dictim structures using multimethods dispatched on element types. Ensures structural correctness and d2 compatibility.

**dictim.utils** - Core utilities for element type detection, key conversion, and primitive parsing shared across the codebase.

### Grammar Architecture

The d2 parser uses a sophisticated approach:
- **Literal definitions** with regex generation to minimize hardcoded patterns
- **Banned character sets** for different contexts (container keys, connection keys, attribute keys)
- **Element precedence order**: `classes | vars | ctr | attr | comment | conn | conn-ref`
- **Transform functions** convert parse trees to dictim data structures

### Key Design Decisions

**Directives**: Top-level ambiguous patterns like `**: suspend` are parsed as attributes (directives) by design, since d2 syntax is ambiguous but dictim differentiates containers from attributes.

**Element Types**: Uses multimethod dispatch on `:tag` metadata to handle different dictim element types during compilation and validation.

**Glob Support**: Glob patterns (`*`, `**`) are supported in attribute keys but banned in container keys to prevent parser ambiguity.

## Current Compatibility

- d2 version: 0.6.9 (upgrading to 0.7.0 with C4 model support)
- Supports vars, classes, connection references, quoted keys, suspend/unsuspend, c4-person shape
- Import syntax (`@file`, `...@file`) parsing is in development

## Development Notes

When modifying the grammar:
- Test changes thoroughly as parser ambiguity can cause multiple parse trees
- Use `parses-d2` function for debugging parse trees instead of `dictim`
- Grammar changes often require corresponding updates to transform functions and compilation logic
- Element type changes need updates across validation, compilation, and utility functions