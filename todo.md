## TODO


## New Features added

- [ ] wiki documentation
- [x] globs
- [x] classes
- [x] positions
- [x] vars
- [x] case insensitivty for keys (no change)
- [x] overrides (no change)
- [?] imports
- [x] layers, scenarios, steps
- [x] array notation (see classes, globs, SQL tables, vars]
- [x] conn-refs
- [x] & character (globs)
- [x] . character in quoted keys
- [x] parse line returns at end of d2 string
- [x] overrides test. null for connection refernces
- [x] null keyword
- [x] parse periods in single quotes in keys -> think just needed for attrs. not needed. d2 handles
- [x] imports
- [x] write validation for conn-refs
- [x] check UML attr vals can have [] "[]string" or () "(r rune, eof bool)"

### design thoughts

- imports -> no special processing for dictim. if '@' handled, then already handled

- lists and arrays
Two questions:
1. Should lists and arrays be the same concept in the code?
two options.
 - make the same thing
  Pros: simpler for user to understand
  Cons: can I define different contents for the different positions in compilation and parsing?
        e.g. in top position, elems and in nested position labels
 - make different things
  Pros: maybe simpler to implement in code
  Cons: harder for the user to understand

try approach 1. first

2. What should be the form of lists and arrays if the same thing?
[:list 1 2 3]

'(1 2 3) -> can't do this because of conversion to json!

think about json

3. Are arrays just for attr-vals? study. check: vars, classes, sql tables.
answer: vars, classes and sql tables (which are modelled as attrs -> just constraint

4. what is in the arrays?
classes: class keys
vars: substitutions, PK and ...

  
