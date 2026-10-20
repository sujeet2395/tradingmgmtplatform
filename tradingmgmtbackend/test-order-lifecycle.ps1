BASE=http://localhost:8080/api

# 1. Reference data
COMPANY=$(curl -s -X POST $BASE/companies -H 'Content-Type: application/json' \
  -d '{"legalName":"ABC Ltd","cin":"U12345DL2020PLC000001"}' | jq -r .id)

SECURITY=$(curl -s -X POST $BASE/securities -H 'Content-Type: application/json' \
  -d "{\"companyId\":\"$COMPANY\",\"referencePrice\":1250}" | jq -r .id)

BUYER=$(curl -s -X POST $BASE/customers -H 'Content-Type: application/json' \
  -d '{"fullName":"Buyer One"}' | jq -r .id)

SELLER=$(curl -s -X POST $BASE/customers -H 'Content-Type: application/json' \
  -d '{"fullName":"Seller One"}' | jq -r .id)

# 2. Order lifecycle
ORDER=$(curl -s -X POST $BASE/orders -H 'Content-Type: application/json' \
  -d "{\"orderType\":\"BUY\",\"buyerCustomerId\":\"$BUYER\",\"sellerCustomerId\":\"$SELLER\",\"securityId\":\"$SECURITY\",\"quantity\":2000,\"pricePerShare\":1250}" \
  | jq -r .id)

curl -s -X POST $BASE/orders/$ORDER/verify
curl -s -X POST $BASE/orders/$ORDER/confirm

TXN=$(curl -s $BASE/orders/$ORDER | jq -r .id) # or read transactionId from the confirm response's linked transaction

# 3. Payment (cheque)
PAYMENT=$(curl -s -X POST $BASE/transactions/$TXN/payment -H 'Content-Type: application/json' \
  -d '{"amount":2500000}' | jq -r .id)

curl -s -X POST $BASE/payments/$PAYMENT/cheque-received -H 'Content-Type: application/json' \
  -d '{"chequeNumber":"123456","bankName":"HDFC"}'
curl -s -X POST $BASE/payments/$PAYMENT/deposit
curl -s -X POST $BASE/payments/$PAYMENT/verify

# 4. Share transfer
TRANSFER=$(curl -s -X POST $BASE/transactions/$TXN/transfer | jq -r .id)
curl -s -X POST $BASE/transfers/$TRANSFER/submit
curl -s -X POST $BASE/transfers/$TRANSFER/complete

# 5. Confirm it settled, and inspect the audit trail
curl -s $BASE/transactions/$TXN | jq .status   # → "SETTLED"
curl -s $BASE/audit/Transaction/$TXN | jq .