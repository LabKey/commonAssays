##
# Copyright (c) 2011 LabKey Corporation
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
##

# Youyi: using install.packages() may only get version 1.5.2, the latest is 1.9.3 on 7/18/2010
library(drc)

# vectorized
FivePL.t=function (t,param) {
    b=param[1]; c=param[2]; d=param[3]; e=param[4]; f=param[5]
    (d-c)/{1+exp(b*t-b*log(e))}^f+c
}
FivePL.x=function (x,param) {
    b=param[1]; c=param[2]; d=param[3]; e=param[4]; f=param[5]
    (d-c)/{1+(x/e)^b}^f+c
}
FivePL.x.inv = function (y,param) {
    b=param[1]; c=param[2]; d=param[3]; e=param[4]; f=param[5]
    if (y<c) return (-Inf)
    else if (y>d) return (Inf)
    else return ( (((d-c)/(y-c))^(1/f)-1)^(1/b)*e )
}

# dat is a data frame with the same requirement as rumi
plot.fit=function (dat, param, add=F, ...) {
    if (!add) plot(log(fi)~expected_conc,data=dat, log="x", ...)
    t=log(dat$expected_conc)
    ts=seq(min(t), max(t), length=1000)
    lines(exp(ts), FivePL.t(ts, param), ...)
}

plotCurv = function (fit, type="l", ...) {
    t=log(fit$data$expected_conc)
    y=fit$data[,"log(fi)"]
    ts=seq(min(t), max(t), length=1000)
    plot(ts, FivePL.t(ts, coef(fit)), type=type, ...)
    points(t, y, ...)
}

# vectorized
# simulate one curve, return FI
simulate1curve=function(param, t, sd.e=0.1) {
    .mean=FivePL.t(t, param)
    y = rnorm (n=length(.mean), mean=.mean, sd=sd.e)
    exp(y)
}

# translate parameters from Multiplex QT to drm
reparam=function (QT.param){
    c(QT.param[2],QT.param[1],QT.param[1]+QT.param[4],QT.param[3],QT.param[5])
}

# self start functions
ssfct.drc.1.5.2 <- function (dataFra) {
    x <- dataFra[, 1]
    y <- dataFra[, 2]
    startVal <- rep(0, 5)
    startVal[3] <- max(y) + 0.001
    startVal[2] <- min(y) - 0.001
    startVal[5] <- 1
    indexT2 <- (x > 0)
    x2 <- x[indexT2]
    y2 <- y[indexT2]
    startVal[c(1, 4)] <- find.be2(x2, y2, startVal[2] - 0.001,
        startVal[3])
#    print (startVal)
    return(startVal)
}
find.be2 <- function(x, y, c, d)
{
#    myprint (x," ")
#    myprint (y)
#    myprint (c)
#    myprint (d)
    logitTrans <- log((d - y)/(y - c))

    lmFit <- lm(logitTrans ~ log(x))
#        eVal <- exp((-coef(logitFit)[1]/coef(logitFit)[2]))
#        bVal <- coef(logitFit)[2]

    coefVec <- coef(lmFit)
    bVal <- coefVec[2]
    eVal <- exp(-coefVec[1]/bVal)

    return(as.vector(c(bVal, eVal)))
}
ss.fct.via.LL4= function (dataFra) {
    x <- dataFra[, 1]
    y <- dataFra[, 2]
    fit0= drm(y ~ x, fct = LL.4(), weights= y^-.5)
    start=c(coef(fit0), 1)
    return (start)
}

# sometimes when y is MFI, the lower asymptote can be below 0, after taking log, we get NaN
gof.cost=function (x) {
    x=ifelse (is.na(x), 10, x)
    mean(abs(x))
}

# Args:
#   fit is a drc fit object
#   logMFI is a vector of log MFI, which will be averaged, or a single value
# Returs:
#   est, se., ...
#   range of expected.conc if out of range in any way
# Issues
#   We make the assumption that the first two columns of fit$data are conc and MFI, where fit is returned by drm
#   not vectorized
getConc=function(fit, logMFI, verbose=FALSE, check.out.of.range=TRUE) {

    m=length(logMFI) #
    y=mean(logMFI)

    # fill-in value when out of bound
    x.inf = log(max(fit$data[,1]))
    x.ninf = log(min(fit$data[,1]))
    se1 = Inf

    b=coef(fit)[1]; c=coef(fit)[2]; d=coef(fit)[3]; e=coef(fit)[4]; f=coef(fit)[5]
    x = FivePL.x.inv(y, coef(fit))

    if(check.out.of.range) {
        # if MFI outside standards MFI, set of Inf
        if ( y < min(fit$data[,2]) ) return (c("log.conc"=x.ninf, "s.e."=se1, "concentration"=NaN, "lower.bound"=NaN, "upper.bound"=NaN, "s1"=NaN, "s2"=NaN, "se.x"=NaN))
        else if ( y > max(fit$data[,2]) ) return (c("log.conc"=x.inf, "s.e."=se1, "concentration"=NaN, "lower.bound"=NaN, "upper.bound"=NaN, "s1"=NaN, "s2"=NaN, "se.x"=NaN))
        if (verbose) print("pass test 1")

        # if estimated conc outside expected conc, set to Inf
        if ( x < min(fit$data[,1]) ) return (c("log.conc"=x.ninf, "s.e."=se1, "concentration"=NaN, "lower.bound"=NaN, "upper.bound"=NaN, "s1"=NaN, "s2"=NaN, "se.x"=NaN))
        else if ( x > max(fit$data[,1]) ) return (c("log.conc"=x.inf, "s.e."=se1, "concentration"=NaN, "lower.bound"=NaN, "upper.bound"=NaN, "s1"=NaN, "s2"=NaN, "se.x"=NaN))
        if (verbose) print("pass test 3")
    }

    # if estimated conc outside asymptote, set to Inf
    if ( y < c ) return (c("log.conc"=x.ninf, "s.e."=se1, "concentration"=NaN, "lower.bound"=NaN, "upper.bound"=NaN, "s1"=NaN, "s2"=NaN, "se.x"=NaN))
    else if ( y > d ) return (c("log.conc"=x.inf, "s.e."=se1, "concentration"=NaN, "lower.bound"=NaN, "upper.bound"=NaN, "s1"=NaN, "s2"=NaN, "se.x"=NaN))
    if (verbose) print("pass test 2")

    A=((d-c)/(y-c))^(1/f)-1
    B=(d-c)/(y-c)

    dx.db = -log(A)*A^(1/b)*b^(-2)*e
    dx.dc = e/b*A^(1/b-1)*B^(1/f-1) *(1/f)*(d-y)/(y-c)^2
    dx.dd = e/b*A^(1/b-1)*B^(1/f-1) *(1/f)/(y-c)
    dx.de = A^(1/b)
    dx.df = e/b*A^(1/b-1)*B^(1/f) *log(B)*(-1/f^2)
    D.beta= c(dx.db, dx.dc, dx.dd, dx.de, dx.df)
    V.beta = vcov(fit)

    dx.dy = -e/(b*f)*A^{1/b-1}*B^{1/f+1}/(d-c)
    sigma2 = summary(fit)$resVar

#    # debug use
#    plotCurv(fit); abline(h=y); abline(2,dx.dy/x)

    s1=dx.dy^2*sigma2/m /x^2
    s2=(D.beta%*%V.beta%*%D.beta) / x^2
    se.t = sqrt(s1 + s2)
    se.x = x*se.t
    return ( c("log.conc"=unname(log(x)), "s.e."=se.t, "concentration"=unname(x), "lower.bound"=exp(log(x)-2*se.t), "upper.bound"=exp(log(x)+2*se.t), "s1"=unname(s1), "s2"=unname(s2), "se.x"=unname(se.x) ))
}

# x is a vector of observations from first group, y is a vector of observations from second group, x.sd is a vector of sd for x
# return AUC without adjustment, and adjusting for error
AUC.e=function (x,y, x.sd){
    x.sd=x.sd[!is.na(x)]
    x=x[!is.na(x)]
    y=y[!is.na(y)]
    c(mean(outer(x,y,"<")),
      mean(outer (1:length(x), y, function (i,y1){
        pnorm(q=y1, mean=x[i], sd=x.sd[i], lower.tail=T)
      }))
    )
}

# fit a concave (default) or convex, montoically increasing nonparametric least square fit
# x and y are both vectors
convexcave.incr.ls = function (x,y, concave=T, convex=F) {

    n=length(x)
    A=matrix(0,n,2*n)
    for (i in 1:n) {
        A[i,2*i-1]=1
        A[i,2*i]=x[i]
    }

    G=matrix(0,n*n, 2*n)
    for (i in 1:n) {
        G[i,2*i]=1
    }
    for (i in 1:n) {
        cursor=0
        for (j in setdiff(1:n, i)) {
            cursor=cursor+1
            G[n+(i-1)*(n-1)+cursor,2*i-1]=-1
            G[n+(i-1)*(n-1)+cursor,2*i]  =-x[i]
            G[n+(i-1)*(n-1)+cursor,2*j-1]=1
            G[n+(i-1)*(n-1)+cursor,2*j]  =x[i]
        }
    }
    if (!concave | convex) G[(n+1):(n*n),]=-G[(n+1):(n*n),]

    require (limSolve)
    cf=lsei ( A=A, B=y, E=NULL, F=NULL, G=G, H=rep(0,n*n))$X
    y.fit = A %*% cf

#    require(quadprog)
#    #solve.QP(Dmat,dvec,Amat,bvec=bvec)
#    # Dmat not full rank

#    require(kernlab)
#    H <- Dmat
#    c <- -dvec
#    A <- t(Amat)
#    b <- bvec
#    l <- matrix(rep(0,2*n))
#    u <- matrix(rep(1e4,2*n))
#    r <- matrix(rep(1e4,n*n))
#    sv <- ipop(c,H,A,b,l,u,r)
#    # Error: system is computationally singular

}

sigmoid.incr.ls = function (x,y,k) {

    require (limSolve)

    n=length(x)
    A=matrix(0,n,2*n)
    for (i in 1:n) {
        A[i,2*i-1]=1
        A[i,2*i]=x[i]
    }

    # beta are positive
    G.1=matrix(0,n, 2*n)
    for (i in 1:n) {
        G.1[i,2*i]=1
    }

    # convex part
    G.2=matrix(0,(k+1)*k,2*n)
    for (i in 1:(k+1)) {
        cursor=0
        for (j in setdiff(1:(k+1), i)) {
            cursor=cursor+1
            G.2[(i-1)*k+cursor,2*i-1]=-1
            G.2[(i-1)*k+cursor,2*i]  =-x[i]
            G.2[(i-1)*k+cursor,2*j-1]=1
            G.2[(i-1)*k+cursor,2*j]  =x[i]
        }
    }
    G.2=-G.2 # convexify

    # concave part
    G.3=matrix(0,(n-k+1)*(n-k),2*n)
    for (i in k:n) {
        cursor=0
        for (j in setdiff(k:n, i)) {
            cursor=cursor+1
            G.3[(i-k)*(n-k)+cursor,2*i-1]=-1
            G.3[(i-k)*(n-k)+cursor,2*i]  =-x[i]
            G.3[(i-k)*(n-k)+cursor,2*j-1]=1
            G.3[(i-k)*(n-k)+cursor,2*j]  =x[i]
        }
    }

    # equality by inequality
    G.4=matrix(0,4,2*n)
    G.4[1,2*k-1]= 1; G.4[1,2*k+1]=-1
    G.4[2,2*k-1]=-1; G.4[2,2*k+1]= 1
    G.4[3,2*k  ]= 1; G.4[3,2*k+2]=-1
    G.4[4,2*k  ]=-1; G.4[4,2*k+2]= 1
    G=rbind(G.1, G.2, G.3, G.4)
    cf=try(lsei (type=2, A=A, B=y, E=NULL, F=NULL, G=G, H=rep(0,nrow(G)))$X)
    if (!inherits(cf, "try-error")) {
        y.fit.1 = A %*% cf
    } else {
        y.fit.1 = rep(mean(y), n)
    }

    # equality by equality
    E=matrix(0,2,2*n)
    E[1,2*k-1]= 1; E[1,2*k+1]=-1
    E[2,2*k  ]= 1; E[2,2*k+2]=-1
    G=rbind(G.1, G.2, G.3)
    cf=try(lsei (A=A, B=y, E=E, F=rep(0,nrow(E)), G=G, H=rep(0,nrow(G)))$X)
    if (!inherits(cf, "try-error")) {
        y.fit.2 = A %*% cf
    } else {
        y.fit.2 = rep(mean(y), n)
    }

    if(sum((y-y.fit.1)**2) < sum((y-y.fit.2)**2))
        return (y.fit.1)
    else
        return (y.fit.2)

#    # checking constraints
#    all(G%*%cf+1e-10>0) # add 1e-10 for R rounding errors
#    cf.1=cf
#    cf.1[3:4]=cf.1[1:2]
#    cf.1[5:6]=coef(lm(y[3:4]~x[3:4]))
#    cf.1[5:6]=cf.1[7:8]
#    for (ii in c(9,11,13,15,17) )     cf.1[ii:(ii+1)]=cf.1[7:8]
#    all(G%*%cf.1+1e-10>0) # add 1e-10 for R rounding errors

}

# better ssfct to get better fits
# Use gof.threshold to report lack of fit
# this is only working for log transformed
# this is not working for weighting for two reasons 1) if weights are present, fails 2) gof needs to be computed differently
fit.drc=function (..., force.fit=F) {

    gof.threshold=.2 # .2 is empirically determined
    control=drmc(maxIt=5000, method="BFGS", relTol=1e-7, trace=F)

    gofs=rep(Inf, 3)
    fits=list()

    fit1= try(drm(fct = LL.5(ssfct=ss.fct.via.LL4), control=control, ...), silent=T)
    if (!inherits(fit1, "try-error")) {
        bad.se = any(diag(vcov(fit1))<0)
        if (bad.se) gof1=Inf
        else gof1 = gof.cost( resid(fit1) )
    } else {
        gof1=Inf
    }
    gofs[1]=gof1
    fits[[1]]=fit1

    if (gof1>gof.threshold) {

        print("ss.fct.via.LL4 fails, try ssfct.drc.1.5.2")
        fit2= try(drm(fct = LL.5(ssfct=ssfct.drc.1.5.2), control=control, ...), silent=T)
        if (!inherits(fit2, "try-error")) {
            bad.se = any(diag(vcov(fit2))<0)
            if (bad.se) gof2=Inf
            else gof2 = gof.cost( resid(fit2) )
        } else gof2=Inf
        gofs[2]=gof2
        fits[[2]]=fit2

        if (gof2>gof.threshold) {

            print("ssfct.drc.1.5.2 fails, try default ssfct")
            fit3 = try(drm(fct = LL.5(), control=control, ...), silent=T)
            if (!inherits(fit3, "try-error")) {
                bad.se = any(diag(vcov(fit3))<0)
                if (bad.se) gof3=Inf
                else gof3 = gof.cost( resid(fit3) )
            } else gof3=Inf
            gofs[3]=gof3
            fits[[3]]=fit3
            if (gof3>gof.threshold) print ("fits failed")
            else print("worked")

        } else print("worked")

    }

    fit=fits[[which.min(gofs)]]
    if (force.fit) {
        return (fit)
    } else {
        if (min(gofs)>gof.threshold) return (NULL)
        else return (fit)
    }
}
#fit.drc(MFI ~ Expected.Conc, data = tt)
#fit.drc(MFI ~ Expected.Conc, data = tt, weights= tt$MFI.avgY^-1)

rumi = function (dat,
    plot=TRUE, auto.layout=TRUE, plot.se.profile=TRUE,
    force.fit=FALSE, test.lod=FALSE,
    find.LOQ=FALSE, find.best.dilution=FALSE, return.fits=FALSE,
    grid.len=NULL, unk.replicate=NULL, verbose=FALSE, ...)
{

    # convert bead_type to analyte
    if (is.null(dat$analyte) & !is.null(dat$bead_type)) dat$analyte=dat$bead_type

    # checking the data
    if (is.null(dat$sample_id)) {cat("ERROR: assay_id missing from dat\n\n"); stop()}
    if (is.null(dat$assay_id)) {cat("ERROR: assay_id missing from dat\n\n"); stop()}
    if (is.null(dat$analyte)) {cat("ERROR: analyte missing from dat\n\n"); stop()}
    if (is.null(dat$well_role)) {cat("ERROR: well_role missing from dat\n\n"); stop()}
    if (is.null(dat$fi)) {cat("ERROR: fi missing from dat\n\n"); stop()}
    if (is.null(dat$starting_conc) & is.null(dat$expected_conc)) {cat("ERROR: starting_conc and expected_conc missing from dat\n\n"); stop()}
    if (!is.numeric(dat$fi)) {cat("ERROR: dat$fi not numeric\n\n"); stop()}

    # set grid length for computing error profile
    if (is.null(grid.len) & plot.se.profile) grid.len=250
    if (is.null(grid.len) & find.LOQ) grid.len=1000

    if (!plot) plot.se.profile=F

    ana=sort(unique(dat$analyte))
    #ana=sort(setdiff(unique(dat$analyte), "blank"))
    assays=sort(setdiff(unique(dat$assay_id), "blank"))
    high.low=NULL
    out=data.frame()
    fits=list()
    for (p in assays) {
        myprint (p)
        for (a in ana) {

            if (auto.layout) {
                if (plot.se.profile) par(mfrow=c(2,2)) else par(mfrow=c(1,1))
            }

            if (verbose) myprint(a)
            dat.a.p=subset(dat, assay_id==p & analyte==a)
            if (nrow(dat.a.p)==0) next

            dat.std=subset(dat.a.p, well_role=="Standard" )
            if (is.null(dat.std$expected_conc)) dat.std$expected_conc=dat.std$starting_conc/dat.std$dilution
            if (nrow(dat.std)==0) next
            std.low =log(min (dat.std$expected_conc))
            std.high=log(max (dat.std$expected_conc))

            fit= fit.drc(log(fi) ~ expected_conc, data = dat.std, force.fit=force.fit)
            if (return.fits) fits[[p%+%a]]=fit

            if (is.null(fit) & plot) {

                plot (fi ~ expected_conc, data = dat.std, log="xy", main="FAILED: "%+%p%+%", "%+%a, cex=.1)
                if (plot.se.profile) empty.plot()

            } else {

                if (plot) plot(fit, type="all", main=p%+%", "%+%a, cex=.5, xlim=c(min(fit$data[,1]), max(fit$data[,1])), ...)

                # sometimes, even though fit is not NULL, some standard errors of the parameter estimates are negative
                bad.se = any(diag(vcov(fit))<0)
                if (bad.se & plot.se.profile) empty.plot()

                # estimate unknown concentrations
                dat.unk=subset(dat.a.p, well_role!="Standard")
                sam = unique(dat.unk$sample_id)
                if (!is.null(sam)) sam=sort(sam)
                for (s in sam) {
                    dil=sort(unlist(unique(subset(dat.unk, sample_id==s, select=dilution))))
                    out.s=data.frame()
                    for (d in dil) {
                        # for each sample/dilution, there may be replicates
                        dat.unk.s.d = subset(dat.unk, dilution == d & sample_id == s)
                        if (is.null(unk.replicate)) unk.replicate=nrow(dat.unk.s.d)
                        estimated = unname(getConc(fit, log(dat.unk.s.d$fi)))

                        if (!bad.se) {

                            # test limits of detection
                            if (test.lod) {
                                test.1.not.rejected=(estimated[1] - std.low)/estimated[2] < 1.64 | estimated[2]==Inf
                                test.2.not.rejected=(std.high - estimated[1])/estimated[2] < 1.64 | estimated[2]==Inf
                                if(test.1.not.rejected & !test.2.not.rejected) estimated[1] = std.low
                                if(test.2.not.rejected & !test.1.not.rejected) estimated[1] = std.high
                                # if both rejected, set to the closer one
                                if(test.1.not.rejected & test.2.not.rejected) estimated[1] = ifelse (estimated[1] > log((exp(std.low)+exp(std.high))/2), std.high, std.low)
                                # set std err to Inf for those touched
                                if(test.1.not.rejected | test.2.not.rejected) estimated[2] = Inf
                            }

                            out.s=rbind (out.s, data.frame (c(dat.unk.s.d[1,], "est.log.conc"=estimated[1]+log(d), "se"=estimated[2])))
                        } else {
                            out.s=rbind (out.s, data.frame (c(dat.unk.s.d[1,], "est.log.conc"=estimated[1]+log(d), "se"=NA)))
                        }
                    }
                    if (!find.best.dilution) {
                        if(length(dil)>1) warning ("There are most than one dilutions for this sample, and we are returning all. sample_id: "%+%s)
                        out=rbind(out, out.s)
                    } else {
                        out=rbind(out, out.s[which.min(out.s$se), ])
                    }
                }

                # if there is no sample, unk.replicate may still be null
                if (is.null(unk.replicate)) unk.replicate=1

                # plot se profile file and others
                if (!bad.se & (find.LOQ | plot.se.profile)) {
                    # compute x.high and x.low
                    fit.low=predict(fit, data.frame(expected_conc=min(fit$data[,1])))
                    fit.high=predict(fit, data.frame(expected_conc=max(fit$data[,1])))
                    if (fit.low>fit.high) next
                    mid.log.fi = unname((fit.low+fit.high)/2)
                    d.95=1.96
                    d.99=2.575829
                    # low end
                    x.hat.low=mysapply(seq(fit.low, mid.log.fi, length=grid.len+1)[-1], function (x) getConc(fit, rep(x,unk.replicate))[c(1,2,6,7,8,4,5)])
                    same.as.low=x.hat.low[,1]-d.95*x.hat.low[,2]<log(min(dat.std$expected_conc))
                    same.as.low.rle=rle(same.as.low)
                    tmp=nrow(x.hat.low)-last(same.as.low.rle$lengths)
                    x.low=x.hat.low[ifelse(tmp>0,tmp,1), 1]
                    # high end
                    x.hat.high=mysapply(seq(fit.high, mid.log.fi, length=grid.len+1)[-1], function (x) getConc(fit, rep(x,unk.replicate))[c(1,2,6,7,8,4,5)])
                    same.as.high=x.hat.high[,1]+d.95*x.hat.high[,2]>log(max(dat.std$expected_conc))
                    same.as.high.rle=rle(same.as.high)
                    tmp=nrow(x.hat.high)-last(same.as.high.rle$lengths)
                    x.high=x.hat.high[ifelse(tmp>0,tmp,1), 1]
                    high.low=rbind(high.low,c(p,a,x.high,x.low))
                    # plot vertical lines at x.high and x.low
                    if (find.LOQ) {
                        abline(v=exp(x.low))
                        abline(v=exp(x.high))
                    }
                    # plot error profile
                    if (plot.se.profile) {
                        tmp=rbind(x.hat.low, x.hat.high[nrow(x.hat.high):1,])
                        tmp=tmp[tmp[,2]!=Inf & !is.na(tmp[,2]),]

                        plot(tmp[,1], tmp[,2],type="n", xlab="Estimated log conc", ylab="S.E. ("%+%unk.replicate%+%" replicates)", log="", ylim=c(0,max(tmp[,2])), xlim=log(c(min(fit$data[,1]), max(fit$data[,1]))))
                        lines(tmp[,1], sqrt(tmp[,4]),type="l", col="blue")
                        lines(tmp[,1], sqrt(tmp[,3]),type="l", col="red")
                        lines(tmp[,1], tmp[,2],type="l", col="black")
                        legend(legend=c("total","replication sensitive","replication insensitive"),col=c("black","red","blue"),lty=1,x="topright",bty="n", cex=.5)

                        # define se profile as percent cv vs estimated conc
                        plot(exp(tmp[,1]), tmp[,5]/exp(tmp[,1])*100,type="l", xlab="Estimated conc", ylab="% CV ("%+%unk.replicate%+%" replicates)", log="x", ylim=c(0,100), xlim=c(min(fit$data[,1]), max(fit$data[,1])))
                        abline(h=20)

#                        # define se profile as percent cv vs estimated conc, using length of CI, similar to last
#                        plot(exp(tmp[,1]), (tmp[,7]-tmp[,6])/4/exp(tmp[,1])*100,type="l", xlab="Estimated log conc", ylab="% CV ("%+%unk.replicate%+%" replicates)", log="x", ylim=c(0,100))
#                        abline(h=20)

                    }
                }
            }
        }
    }
    if (find.LOQ) {
        high.low=as.data.frame(high.low)
        names(high.low)=c("assay","analyte","x.high","x.low")
        high.low$x.high=as.numeric(high.low$x.high)
        high.low$x.low=as.numeric(high.low$x.low)
        attr(out, "high.low")=high.low
    }
    if (return.fits) attr(out, "fits")=fits
    return (out)
}


# fit curves with jags
# dat is a data frame with similar expections as the input to rumi
fit.w.jags=function (dat, model="R", hyp.id=2) {

    # do drm fits, fitted parameter will be used to get initial values for samplers
    fits = attr(rumi(dat, return.fits=T, force.fit=T, plot.se.profile=F, auto.layout=F, plot=F), "fits")
    params.drm = sapply(fits, mycoef)
    sd.resid=(sqrt(sapply(fits, function (fit) summary(fit)$resVar)))

    if(hyp.id==2) {
        load(file="hyperparameters1.Rdata")
    } else if (hyp.id==1) {
        load(file="hyperparametersEMP.Rdata")
    } else stop ("hyp.id not supported")

    dat$assay=as.numeric(as.factor(dat$assay_id))
    cntAssays=length(unique(dat$assay_id))

    require(rjags)
    pri.id=3
    jags.data = list("t"=log(dat$expected_conc), "y"=log(dat$fi), "assay"=dat$assay, "I"=cntAssays, "K"=nrow(dat), "priors.random.effect"=priors.random.effect, "priors.fixed.effect"=priors.fixed.effect, "dof"=4)
    jags.inits = list(
        "log.minus.b"=log(-params.drm["b",]), "log.minus.b.0"=priors.random.effect["b",1], "tau.log.minus.b"=priors.random.effect["b",3]/priors.random.effect["b",4],
        "c"=params.drm["c",],                 "c.0"=priors.random.effect["c",1],           "tau.c"=priors.random.effect["c",3]/priors.random.effect["c",4],
        "d"=params.drm["d",],                 "d.0"=priors.random.effect["d",1],           "tau.d"=priors.random.effect["d",3]/priors.random.effect["d",4],
        "log.e"=log(params.drm["e",]),        "log.e.0"=priors.random.effect["e",1],       "tau.log.e"=priors.random.effect["e",3]/priors.random.effect["e",4],
        "log.f"=log(params.drm["f",]),        "log.f.0"=priors.random.effect["f",1],       "tau.log.f"=priors.random.effect["f",3]/priors.random.effect["f",4],
        "tau"=sd.resid**-2, .RNG.name="base::Mersenne-Twister", .RNG.seed=1
    )

    n.iter=1e5; n.thin=1; n.burnin=n.iter/n.thin/100
    jags.model.1 = jags.model(file="pri"%+%pri.id%+%"_model"%+%model%+%".txt", data=jags.data, inits=jags.inits, n.chain = 1, n.adapt=1e3)
    samples = coda.samples(jags.model.1, c(letters[2:6],"sigma2"), n.iter=n.iter, thin = n.thin )[[1]][-(1:n.burnin),,drop=FALSE]

    est=mysapply (1:cntAssays, function (i) {
        tmp=apply(samples, 2, median)
        param.post.median = tmp[(1:5-1)*cntAssays+i]
        sd.resid.post.median=sqrt(tmp[5*cntAssays+i])
        out=c(param.post.median, sd.resid.post.median)
        names(out)=c("b","c","d","e","f","sigma")
        out
    })
    rownames(est)=levels(as.factor(dat$assay_id))
    est

}