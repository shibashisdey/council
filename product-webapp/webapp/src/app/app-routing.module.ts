import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { AuthGuard } from './core/auth.guard';
import { HomeComponent } from './pages/home/home.component';
import { ProfileSetupComponent } from './pages/setup/profile-setup.component';
import { ScheduleSetupComponent } from './pages/setup/schedule-setup.component';
import { ProfileGuard } from './core/profile.guard';
import { ProfileSetupGuard } from './core/profile-setup.guard';
import { ScheduleSetupGuard } from './core/schedule-setup.guard';

const routes: Routes = [
  { path: '', component: HomeComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [AuthGuard, ProfileGuard] },
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
