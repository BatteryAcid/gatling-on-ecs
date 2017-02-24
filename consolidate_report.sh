#!/bin/bash
function help_text {
    cat <<EOF
    Usage: $0 [ -p|--profile PROFILE ] [ -r|--report-bucket REPORT_BUCKET ] [-h]
        
        PROFILE         (optional) The profile to use from ~/.aws/credentials.
        REPORT_BUCKET   (required) name of the S3 bucket to upload the reports to. Must be in same AWS account as profile.
                                   It must be provided.
EOF
    exit 1
}

while [ $# -gt 0 ]; do
    arg=$1
    case $arg in
        -h|--help)
            help_text
        ;;
        -p|--profile)
            export AWS_DEFAULT_PROFILE="$2"
            shift; shift
        ;;
        -r|--report-bucket)
            REPORT_BUCKET="$2"
            shift; shift
        ;;
        *)
            echo "ERROR: Unrecognised option: ${arg}"
            help_text
            exit 1
        ;;
    esac
done


if [ -z "$REPORT_BUCKET" ]
then
    echo "Report bucket required. Please make sure its empty."
    help_text
    exit 1
fi

rm -r target/gatling/*
## Append fake timespatm over simulation so sbt tasks picks up automatically for report generation   
dir_name="tilesimulation"-`jot -r -n 13 0 9 | rs -g 0`
mkdir -p target/gatling/$dir_name
## Download all reports for all test gatling clients
aws s3 cp s3://${REPORT_BUCKET}/ target/gatling/$dir_name --recursive

## Consolidate reports from these clients
sbt gatling:generateReport

## Uplaod consolidated gatling html reports back to s3
aws s3 cp target/gatling/$dir_name s3://${REPORT_BUCKET}/Consolidated_Reports --recursive
