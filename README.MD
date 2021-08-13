# 10x Banking APP

This is an application developed in Java language and runs on the Spring framework.

This application exposes API's via port 8080.


# You can test this by viewing http://localhost:8080/ in a browser

#### Method 1
**Url:**
 
    localhost:8080/getaccountstatus

**type:**
    
    GET

**description:**
    
    Returns all the accounts and their attributes similar to the below example.
    [{id:2220222, balance: 100000.0, currency: "GBP", createdAt: "yyyy-MM-dd HH:mm:ss"}, {...}, ...]

#### Method 2
**Url:**

    localhost:8080/getaccountstatus/{id}

**type:**

    GET

**description:**

    Returns an account and its attributes similar to the below example.
    {id:2220222, balance: 100000.0, currency: "GBP", createdAt: "yyyy-MM-dd HH:mm:ss"}

#### Method 3
**Url:**

    localhost:8080/createaccount

**type:**

    POST

**body:**
    
    {
    "id": "2220222",
    "balance": 100000,
    "currency": "GBP"
    }
    
**description:**

    Returns the request status. 

#### Method 4
**Url:**

    localhost:8080/transfer

**type:**

    POST

**body:**

    {
    "sourceAccountId": 2220222,
    "destinationAccountId": 10101010,
    "amount": 10,
    "currency": "GBP"
    }

**description:**

    Returns the request status and the transaction ID if successful.

#### Method 5
**Url:**

    localhost:8080/clearalldata/{tablename}

**type:**

    GET

**description:**

    Deletes the data in the specified table.  