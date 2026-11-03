/**
 * Base words of the most common passwords. Keep in sync with
 * backend/src/main/resources/validation/common-passwords.txt (the server checks the same list).
 */
export const COMMON_PASSWORDS = new Set([
  'password', 'passw0rd', 'pass', 'qwerty', 'qwertyuiop', 'asdfgh', 'asdfghjkl', 'zxcvbnm', 'letmein', 'welcome',
  'admin', 'administrator', 'login', 'iloveyou', 'monkey', 'dragon', 'master', 'sunshine', 'princess', 'football',
  'baseball', 'cricket', 'shadow', 'superman', 'batman', 'trustno', 'abc', 'abcd', 'abcde', 'abcdef', 'abcdefg',
  'abcdefgh', 'hello', 'freedom', 'whatever', 'secret', 'guest', 'test', 'testing', 'user', 'default', 'changeme',
  'library', 'shelfspace', 'books', 'india', 'computer', 'internet', 'summer', 'winter', 'starwars', 'pokemon',
  'google', 'samsung',
])
