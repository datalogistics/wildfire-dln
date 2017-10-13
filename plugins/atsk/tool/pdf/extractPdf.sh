#!/bin/sh

for FILE in *.pdf;do \
    pdftk "${FILE}" dump_data_fields > /tmp/x
    java Extract /tmp/x > "${FILE}".csv;done
