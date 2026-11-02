import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import { BookCover } from './BookCover'

describe('BookCover', () => {
  it('draws the title and author, never an outside image', () => {
    const { container } = render(<BookCover title="Clean Code" author="Robert C. Martin" isbn="9780132350884" />)
    expect(screen.getByText('Clean Code')).toBeInTheDocument()
    expect(screen.getByText('Robert C. Martin')).toBeInTheDocument()
    expect(container.querySelector('img')).toBeNull()
  })

  it('gives a book the same colour every time', () => {
    const colour = (title: string) => (render(<BookCover title={title} />).container.firstChild as HTMLElement).style.background
    expect(colour('Sapiens')).toBe(colour('Sapiens'))
  })
})
