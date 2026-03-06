import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { AuthGuard } from './core/auth.guard';
import { HomeComponent } from './pages/home/home.component';
import { ProfileSetupComponent } from './pages/setup/profile-setup.component';
import { ScheduleSetupComponent } from './pages/setup/schedule-setup.component';
import { UserProfileComponent } from './pages/profile/user-profile.component';
import { CounselorProfileComponent } from './pages/profile/counselor-profile.component';
import { CounselorListComponent } from './pages/counselors/counselor-list.component';
import { CounselorDetailComponent } from './pages/counselors/counselor-detail.component';
import { ClientAppointmentsComponent } from './pages/appointments/client-appointments.component';
import { CounselorSessionsComponent } from './pages/sessions/counselor-sessions.component';
import { PaymentCheckoutComponent } from './pages/payments/payment-checkout.component';
import { NotesComponent } from './pages/notes/notes.component';
import { ProfileGuard } from './core/profile.guard';
import { ProfileSetupGuard } from './core/profile-setup.guard';
import { ScheduleSetupGuard } from './core/schedule-setup.guard';

const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [AuthGuard, ProfileGuard] },
  { path: 'counselors', component: CounselorListComponent, canActivate: [AuthGuard, ProfileGuard] },
  { path: 'counselors/:id', component: CounselorDetailComponent, canActivate: [AuthGuard, ProfileGuard] },
  { path: 'appointments', component: ClientAppointmentsComponent, canActivate: [AuthGuard, ProfileGuard] },
  { path: 'payments/:appointmentId', component: PaymentCheckoutComponent, canActivate: [AuthGuard, ProfileGuard] },
  { path: 'notes', component: NotesComponent, canActivate: [AuthGuard, ProfileGuard] },
  { path: 'sessions', component: CounselorSessionsComponent, canActivate: [AuthGuard, ProfileGuard] },
  { path: 'profile/user', component: UserProfileComponent, canActivate: [AuthGuard, ProfileGuard] },
  { path: 'profile/counselor', component: CounselorProfileComponent, canActivate: [AuthGuard, ProfileGuard] },
  { path: 'setup/profile', component: ProfileSetupComponent, canActivate: [AuthGuard, ProfileSetupGuard] },
  { path: 'setup/schedule', component: ScheduleSetupComponent, canActivate: [AuthGuard, ScheduleSetupGuard] },
  { path: 'auth', loadChildren: () => import('./auth/auth.module').then(m => m.AuthModule) },
  { path: '**', redirectTo: '' }
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
