aws logs describe-log-groups \
    --query 'logGroups[?storedBytes == `0`].logGroupName' --output text | \
    xargs -r -n1 aws logs delete-log-group --log-group-name
