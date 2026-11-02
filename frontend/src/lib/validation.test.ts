import { describe, expect, it } from 'vitest'
import {
  costProblem, emailProblem, isbnProblem, nameProblem, passwordChecks, passwordProblem, passwordStrength, phoneProblem, yearProblem,
} from './validation'

describe('passwords (same rules as the server)', () => {
  it('accepts strong passwords', () => {
    for (const pw of ['Shelf2Read', 'Brand1New', 'Correct-Horse-9']) expect(passwordProblem(pw)).toBeNull()
  })

  it('names the first rule that fails', () => {
    expect(passwordProblem('')).toBe('Enter a password')
    expect(passwordProblem('Ab1')).toBe('Must be 8-72 characters')
    expect(passwordProblem(' Shelf2Read')).toBe('Must not start or end with a space')
    expect(passwordProblem('shelf2read')).toBe('Needs both an uppercase and a lowercase letter')
    expect(passwordProblem('ShelfReads')).toBe('Needs at least one number')
  })

  it('refuses common passwords even with numbers and symbols around them', () => {
    for (const pw of ['Password1', 'Password123!', 'Welcome123', 'Admin@12345', '123Letmein!', 'Passw0rd1']) {
      expect(passwordProblem(pw)).toBe('Too common. Choose something harder to guess')
    }
  })

  it("refuses the person's name or email name", () => {
    const person = { name: 'Ananya Iyer', email: 'a.iyer@example.com' }
    expect(passwordProblem('Ananya2026x', person)).toBe('Must not contain your name or email')
    expect(passwordProblem('My-a.iyer-7', person)).toBe('Must not contain your name or email')
    expect(passwordProblem('Shelf2Read', person)).toBeNull()
  })

  it('rates strength and lists each rule', () => {
    expect(passwordStrength('Shelf2Read')).toBe('good')
    expect(passwordStrength('Shelf2Read-Daily')).toBe('strong')
    expect(passwordStrength('shelf')).toBe('weak')
    const checks = passwordChecks('shelf2read', { name: 'Ananya' })
    expect(checks.map((c) => c.ok)).toEqual([true, false, true, true, true])
  })
})

describe('other fields', () => {
  it('names', () => {
    expect(nameProblem("Zoë O'Brien-Nair")).toBeNull()
    expect(nameProblem('  ')).toBe('Enter a name')
    expect(nameProblem('A')).toBe('Must be 2-100 characters')
    expect(nameProblem('12345')).toBe('Use letters, spaces, apostrophes, hyphens and dots only')
  })

  it('emails need a real-looking domain', () => {
    expect(emailProblem('ananya@example.co.in')).toBeNull()
    expect(emailProblem('ananya@gmail')).toBe('Enter a valid email address, like name@example.com')
    expect(emailProblem('ananya@@gmail.com')).not.toBeNull()
    expect(emailProblem('an..anya@gmail.com')).not.toBeNull()
  })

  it('phone is optional but must be digits', () => {
    expect(phoneProblem('')).toBeNull()
    expect(phoneProblem('+919876543210')).toBeNull()
    expect(phoneProblem('98765')).toBe('Must be 10-15 digits, optionally starting with +')
  })

  it('ISBNs are checked with their check digit', () => {
    expect(isbnProblem('978-0-13-235088-4')).toBeNull()
    expect(isbnProblem('0132350882')).toBeNull()
    expect(isbnProblem('9780132350885')).toBe('Not a valid ISBN-10 or ISBN-13. Check the digits')
  })

  it("years can't be far in the future", () => {
    const now = new Date('2026-11-03')
    expect(yearProblem('2027', now)).toBeNull()
    expect(yearProblem('2028', now)).toBe("Can't be later than next year")
    expect(yearProblem('1200', now)).toBe('Must be 1450 or later')
    expect(yearProblem('', now)).toBeNull()
  })

  it('replacement cost', () => {
    expect(costProblem('450.50')).toBeNull()
    expect(costProblem('-1')).toBe('Must be 0 or more')
    expect(costProblem('2000000')).toBe('Must be at most 1000000')
    expect(costProblem('4.555')).toBe('Use at most 2 decimal places')
  })
})
