# Data Pool Manager

## Contribute
The data pool manager, DPM, for consolidating and facilitating requests to Fermilab Control System.

* Clone the code
```
git clone https://github.com/fermi-ad/data-pool-manager.git
```
* Create a branch
```
git branch new-branch
```
* Checkout that branch
```
git checkout -b new-branch
```
* Update the code
```
vim xyz.java
```
* Commit the code
```
git add .
git commit -m "Comment for the code"
```
* Push this updated code to the repository
```
git push -u origin new-branch
```
* Go to the web-page and create pull request
* Add reviewer

## Checking for Bugs

We use SpotBugs to identify common bugs and improve code quality.

Run the following command to check for bugs:

```mvn site```

Once complete, the report will be generated at `target/spotbugs.html`
For more information about SpotBugs, see https://spotbugs.github.io/