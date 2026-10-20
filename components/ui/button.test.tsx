import * as React from 'react';
import { render, screen } from '@testing-library/react';
import { Button } from './button';

describe('Button Component', () => {
  it('renders correctly with default props', () => {
    render(<Button>Click me</Button>);
    
    const buttonElement = screen.getByRole('button', { name: /click me/i });
    expect(buttonElement).toBeInTheDocument();
    
    // Verifying it has the default variant classes
    expect(buttonElement).toHaveClass('bg-primary');
    expect(buttonElement).toHaveClass('text-primary-foreground');
  });

  it('applies the correct variant classes', () => {
    render(<Button variant="destructive">Delete</Button>);
    
    const buttonElement = screen.getByRole('button', { name: /delete/i });
    expect(buttonElement).toBeInTheDocument();
    
    // Verifying it has the destructive variant classes
    expect(buttonElement).toHaveClass('bg-destructive/10');
    expect(buttonElement).toHaveClass('text-destructive');
  });

  it('renders as disabled when disabled prop is passed', () => {
    render(<Button disabled>Disabled Button</Button>);
    
    const buttonElement = screen.getByRole('button', { name: /disabled button/i });
    expect(buttonElement).toBeDisabled();
    expect(buttonElement).toHaveClass('disabled:opacity-50');
  });
});
