# Documentum custom API

This app extends Documentum API. In this case we implement:

1. Merge files: Takes a folder and merge all the content in a single file with the same namo of the folder.

To launch merge use:
- http://localhost:8080/documentum-custom/rest/file/merge?filePath=%2FTemp%2Fsubmition_xxxx%2Fexample_file.txt

To test app running:
- http://localhost:8080/documentum-custom/rest/ping