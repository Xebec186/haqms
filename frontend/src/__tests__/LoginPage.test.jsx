import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContext';
import LoginPage from '../../pages/auth/LoginPage';

// Mock react-toastify
jest.mock('react-toastify', () => ({
  toast: {
    success: jest.fn(),
    error:   jest.fn(),
  },
}));

// Mock useNavigate
const mockNavigate = jest.fn();
jest.mock('react-router-dom', () => ({
  ...jest.requireActual('react-router-dom'),
  useNavigate: () => mockNavigate,
  useLocation: () => ({ state: null }),
}));

const mockLogin = jest.fn();

function renderWithAuth() {
  return render(
    <AuthContext.Provider value={{ login: mockLogin }}>
      <MemoryRouter>
        <LoginPage />
      </MemoryRouter>
    </AuthContext.Provider>
  );
}

describe('LoginPage', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('renders username, password fields and sign-in button', () => {
    renderWithAuth();
    expect(screen.getByLabelText(/username/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/password/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
  });

  test('shows required validation errors on empty submit', async () => {
    renderWithAuth();
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));
    await waitFor(() => {
      expect(screen.getByText(/username is required/i)).toBeInTheDocument();
      expect(screen.getByText(/password is required/i)).toBeInTheDocument();
    });
    expect(mockLogin).not.toHaveBeenCalled();
  });

  test('calls login with correct credentials on valid submit', async () => {
    mockLogin.mockResolvedValue({ role: 'PATIENT', userId: 1 });
    renderWithAuth();

    await userEvent.type(screen.getByLabelText(/username/i), 'kwame');
    await userEvent.type(screen.getByLabelText(/password/i), 'password123');
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith({
        username: 'kwame',
        password: 'password123',
      });
    });
  });

  test('navigates to /patient after successful PATIENT login', async () => {
    mockLogin.mockResolvedValue({ role: 'PATIENT', userId: 1 });
    renderWithAuth();

    await userEvent.type(screen.getByLabelText(/username/i), 'patient1');
    await userEvent.type(screen.getByLabelText(/password/i), 'pass1234');
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/patient');
    });
  });

  test('navigates to /provider after successful PROVIDER login', async () => {
    mockLogin.mockResolvedValue({ role: 'PROVIDER', userId: 2 });
    renderWithAuth();

    await userEvent.type(screen.getByLabelText(/username/i), 'drkofi');
    await userEvent.type(screen.getByLabelText(/password/i), 'pass1234');
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/provider');
    });
  });

  test('shows error toast on failed login', async () => {
    const { toast } = require('react-toastify');
    mockLogin.mockRejectedValue({
      response: { data: { message: 'Invalid username or password.' } },
    });
    renderWithAuth();

    await userEvent.type(screen.getByLabelText(/username/i), 'wrong');
    await userEvent.type(screen.getByLabelText(/password/i), 'wrongpass');
    fireEvent.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith('Invalid username or password.');
    });
  });

  test('register link is present', () => {
    renderWithAuth();
    expect(screen.getByRole('link', { name: /register here/i })).toBeInTheDocument();
  });
});
