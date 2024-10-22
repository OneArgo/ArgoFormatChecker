# Argo NetCDF file format checker
The Argo NetCDF file format checker performs format and content checks on Argo NetCDF files.  
The Argo NetCDF format is described in "Argo user's manual" http://dx.doi.org/10.13155/29825  
More information on https://www.argodatamgt.org/Documentation  
The format checker has two directories:
- file_checker_exec : the java code
- file_checker_spec : the rules applied on the NetCDF file by the java code

The rules files implement the Argo vocabularies managed on [NVS vocabulary server](https://vocab.nerc.ac.uk/search_nvs/).  
Vocabularies and format changes are managed on [Argo Vocabs Task Team - AVTT GitHub](https://github.com/orgs/OneArgo/projects/4/views/1).
