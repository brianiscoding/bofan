# Shepherd Money Interview Project

Hello. My name is Brian Tran. I am excited to be considered for this position. 

Changes that I have made include
MODELS
- user has oneToMany relationship with creditCards
- creditCards has oneToMany relationship with balanceHistory
- balanceHistory is stored as a sorted list

CONTROLLERS:
- user create and delete
- creditCards searchByUser, create and connect to user, search all cards by user

UPDATE BALANCE:
- for every update
- check if credit card exists
- user binary search to find closest previous index
- either edit an existing balance with same date or create new entry and insert
- propagate the changes if possible
- fill in gap from first entry to todays date
- for every gap inbetween
- get older update and paste that balance to fill gap
