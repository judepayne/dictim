# How to work on this repo

## Rules

1. Always switch to a new branch for features. The .circleci job is configured to build dictim.jar and release it every time that main is checked in!





## How to do a release

1. switch to main and merge in your branch:

````bash
git checkout main
git merge <feature-branch>
````

- generate the API docs. bb quickdoc
- mv API.md ../dictim.wiki/


2. update the version in resources/VERSION


3. update the CHANGELOG (best to do this in-running using an *unpublished* section).


4. If you've changed content in .circle or .build, git add those two folders


5. git add *, git commit, git tag <new-version> <sha-from-commit>


6. Update the deps info on the README


7. **think a bit**.. have you missed anything


8. git push origin main --tags


9. Wait for circleci to run and check that it's been successful


;; 10. Go to the repo in Github and add notes to the new release and MANUALLY publish it


11. You are done!
