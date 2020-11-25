@param String query
{
  "query" : {
    "query_string" : {
      "query" : "${query}",
      "default_field":"name.first"
    }
  }
}