import { render, screen } from '@testing-library/react';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { AuthContext } from '../../context/AuthContext';
import ProtectedRoute from '../../routes/ProtectedRoute';

function renderWithAuth(authValue, initialPath = '/protected') {
  return render(
    <AuthContext.Provider value={authValue}>
      <MemoryRouter initialEntries={[initialPath]}>
        <Routes>
          <Route path="/login"     element={<div>Login Page</div>} />
          <Route path="/unauthorized" element={<div>Unauthorized</div>} />
          <Route
            path="/protected"
            element={
              <ProtectedRoute allowedRoles={['PATIENT']}>
                <div>Protected Content</div>
              </ProtectedRoute>
            }
          />
        </Routes>
      </MemoryRouter>
    </AuthContext.Provider>
  );
}

describe('ProtectedRoute', () => {

  test('shows content when user is authenticated with correct role', () => {
    renderWithAuth({
      isAuthenticated: true,
      user: { role: 'PATIENT' },
      loading: false,
    });
    expect(screen.getByText('Protected Content')).toBeInTheDocument();
  });

  test('redirects to /login when not authenticated', () => {
    renderWithAuth({
      isAuthenticated: false,
      user: null,
      loading: false,
    });
    expect(screen.getByText('Login Page')).toBeInTheDocument();
    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
  });

  test('redirects to /unauthorized when role is not allowed', () => {
    renderWithAuth({
      isAuthenticated: true,
      user: { role: 'PROVIDER' },
      loading: false,
    });
    expect(screen.getByText('Unauthorized')).toBeInTheDocument();
    expect(screen.queryByText('Protected Content')).not.toBeInTheDocument();
  });
});
