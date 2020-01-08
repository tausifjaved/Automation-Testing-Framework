@Android
Feature: New Payment

  Scenario: New Payment
    * Press "New Payment"
    * Press "standard payment"
    * Type "automated-test" into inputfield with text "Debtor Reference"
    * Type "12345678" into inputfield with text "Credit account"
    * Type "Tausif Javed" into inputfield with text "Creditor name"
    * Type "Lahore/Pakistan" into inputfield with text "Creditor City"
    * Type "automated-test" into inputfield with text "Creditor Reference"
    * Input "100" into inputfield with text "Amount RSD"
    * Type " - AUTOMATED" into inputfield with text "Payment details"
    * Press "Additional Data"
    * Press "urgent payment"
    * Press "Next"
    * If present, Press "Yes"
    * Back
    * Press "compensation payment"
    * Back
    * Back
